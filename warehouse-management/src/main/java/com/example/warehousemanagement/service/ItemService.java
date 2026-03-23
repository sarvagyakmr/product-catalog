package com.example.warehousemanagement.service;

import com.example.commons.dto.InventoryEventDto;
import com.example.commons.enums.InventoryState;
import com.example.warehousemanagement.dto.AddItemToBoxRequest;
import com.example.warehousemanagement.dto.CycleCountRequest;
import com.example.warehousemanagement.dto.ItemCreateRequest;
import com.example.warehousemanagement.dto.PutAwayRequest;
import com.example.warehousemanagement.entity.BoxItem;
import com.example.warehousemanagement.entity.Item;
import com.example.warehousemanagement.entity.StorageBox;
import com.example.warehousemanagement.enums.BoxType;
import com.example.warehousemanagement.enums.ItemStatus;
import com.example.warehousemanagement.repository.BoxItemRepository;
import com.example.warehousemanagement.repository.ItemRepository;
import com.example.warehousemanagement.repository.StorageBoxRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final BoxItemRepository boxItemRepository;
    private final StorageBoxRepository storageBoxRepository;
    private final RedisPublisher redisPublisher;

    public ItemService(ItemRepository itemRepository, BoxItemRepository boxItemRepository, StorageBoxRepository storageBoxRepository, RedisPublisher redisPublisher) {
        this.itemRepository = itemRepository;
        this.boxItemRepository = boxItemRepository;
        this.storageBoxRepository = storageBoxRepository;
        this.redisPublisher = redisPublisher;
    }

    public Item createItem(ItemCreateRequest request) {
        if (request.getWarehouseId() == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        Item item = new Item(request.getProductId(), ItemStatus.CREATED, request.getWarehouseId());
        return itemRepository.save(item);
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    @Transactional
    public BoxItem addItemToBox(AddItemToBoxRequest request) {
        // Get item and validate it's in CREATED state
        Item item = itemRepository.findById(request.getItemId())
            .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + request.getItemId()));

        if (item.getStatus() != ItemStatus.CREATED) {
            throw new IllegalStateException("Item must be in CREATED state to be added to a box");
        }

        // Get box and validate it's of type INWARD
        StorageBox box = storageBoxRepository.findById(request.getBoxId())
            .orElseThrow(() -> new IllegalArgumentException("Box not found with id: " + request.getBoxId()));

        if (box.getType() != BoxType.INWARD) {
            throw new IllegalStateException("Item can only be added to a box of type INWARD");
        }


        // Create BoxItem association
        BoxItem boxItem = new BoxItem(request.getItemId(), request.getBoxId());
        return boxItemRepository.save(boxItem);
    }

    public List<BoxItem> getItemsByBoxId(Long boxId) {
        return boxItemRepository.findByBoxId(boxId);
    }

    @Transactional
    public void completeInwardForBox(Long boxId) {
        // Get the box and validate it exists
        StorageBox box = storageBoxRepository.findById(boxId)
            .orElseThrow(() -> new IllegalArgumentException("Box not found with id: " + boxId));

        // Get all items in the box
        List<BoxItem> boxItems = boxItemRepository.findByBoxId(boxId);

        // Group items by productId and count quantities
        Map<Long, Integer> productQuantities = new HashMap<>();
        for (BoxItem boxItem : boxItems) {
            Item item = itemRepository.findById(boxItem.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + boxItem.getItemId()));
            
            Long productId = item.getProductId();
            productQuantities.merge(productId, 1, Integer::sum);
            
            // Update item status to INWARD
            item.setStatus(ItemStatus.INWARD);
            itemRepository.save(item);
        }

        // Send inventory updates to inventory service for each product
        Long warehouseId = box.getWarehouseId();
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            // Publish inventory create event to Redis
            InventoryEventDto event = InventoryEventDto.createEvent(
                entry.getKey(),
                entry.getValue(),
                warehouseId
            );
            redisPublisher.publishInventoryEvent(event);
        }
    }

    @Transactional
    public void putAwayBox(PutAwayRequest request) {
        // Get the box and validate it exists
        StorageBox box = storageBoxRepository.findById(request.getBoxId())
            .orElseThrow(() -> new IllegalArgumentException("Box not found with id: " + request.getBoxId()));

        // Validate the storage location is empty (no box already on it)
        List<StorageBox> boxesAtLocation = storageBoxRepository.findByLocationId(request.getLocationId());
        if (!boxesAtLocation.isEmpty()) {
            throw new IllegalStateException("Storage location is not empty");
        }

        // Update box with locationId and type to STORAGE
        box.setLocationId(request.getLocationId());
        box.setType(BoxType.STORAGE);
        storageBoxRepository.save(box);

        // Get all items in the box
        List<BoxItem> boxItems = boxItemRepository.findByBoxId(request.getBoxId());

        // Group items by productId and status for inventory updates
        Map<Long, Integer> inwardProductQuantities = new HashMap<>();

        for (BoxItem boxItem : boxItems) {
            Item item = itemRepository.findById(boxItem.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + boxItem.getItemId()));

            if (item.getStatus() == ItemStatus.INWARD) {
                // Update item status to LIVE
                item.setStatus(ItemStatus.LIVE);
                itemRepository.save(item);

                // Track for inventory move
                Long productId = item.getProductId();
                inwardProductQuantities.merge(productId, 1, Integer::sum);
            }
            // If item is already LIVE, do nothing (no inventory update)
        }

        // Send inventory move updates (RECEIVED -> AVAILABLE) for items that were INWARD
        Long warehouseId = box.getWarehouseId();
        for (Map.Entry<Long, Integer> entry : inwardProductQuantities.entrySet()) {
            // Publish inventory move event to Redis (RECEIVED -> AVAILABLE)
            InventoryEventDto event = InventoryEventDto.moveEvent(
                entry.getKey(),
                InventoryState.RECEIVED,
                InventoryState.AVAILABLE,
                entry.getValue(),
                warehouseId
            );
            redisPublisher.publishInventoryEvent(event);
        }
    }

    @Transactional
    public void cycleCount(CycleCountRequest request) {
        // Get the box and validate it exists
        StorageBox box = storageBoxRepository.findById(request.getBoxId())
            .orElseThrow(() -> new IllegalArgumentException("Box not found with id: " + request.getBoxId()));

        // Convert provided itemIds to a Set for quick lookup
        java.util.Set<Long> providedItemIds = new java.util.HashSet<>(request.getItemIds());

        // Get all expected items mapped to this box
        List<BoxItem> expectedBoxItems = boxItemRepository.findByBoxId(request.getBoxId());

        // Track extra items that need inventory update (INWARD -> LIVE)
        Map<Long, Integer> extraInwardQuantities = new HashMap<>();

        // Step 1: Process expected items (mapped to this box)
        for (BoxItem boxItem : expectedBoxItems) {
            Long itemId = boxItem.getItemId();
            if (!providedItemIds.contains(itemId)) {
                // Item expected but not present -> mark as LOST
                Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + itemId));
                item.setStatus(ItemStatus.LOST);
                itemRepository.save(item);
            }
            // If item IS present, keep as is (already LIVE or other status)
        }

        // Step 2: Process provided items (physical items found)
        for (Long itemId : request.getItemIds()) {
            // Check if this item is mapped to this box
            List<BoxItem> existingMappings = boxItemRepository.findByItemId(itemId);
            boolean mappedToThisBox = existingMappings.stream()
                .anyMatch(bi -> bi.getBoxId().equals(request.getBoxId()));

            if (!mappedToThisBox) {
                // Extra item - not mapped to this box
                Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + itemId));

                if (item.getStatus() == ItemStatus.INWARD) {
                    // INWARD extra item -> update to LIVE + inventory update
                    item.setStatus(ItemStatus.LIVE);
                    itemRepository.save(item);

                    Long productId = item.getProductId();
                    extraInwardQuantities.merge(productId, 1, Integer::sum);

                    // Create BoxItem mapping to this box
                    BoxItem newBoxItem = new BoxItem(itemId, request.getBoxId());
                    boxItemRepository.save(newBoxItem);
                } else if (item.getStatus() == ItemStatus.LIVE) {
                    // LIVE extra item mapped to different box -> re-map to current box
                    // Remove old BoxItem(s)
                    for (BoxItem oldBoxItem : existingMappings) {
                        boxItemRepository.delete(oldBoxItem);
                    }
                    // Create new BoxItem for this box
                    BoxItem newBoxItem = new BoxItem(itemId, request.getBoxId());
                    boxItemRepository.save(newBoxItem);
                }
                // If item is already LOST or other status, handle as needed (could add to box)
            }
        }

        // Send inventory updates for extra INWARD items that became LIVE
        Long warehouseId = box.getWarehouseId();
        for (Map.Entry<Long, Integer> entry : extraInwardQuantities.entrySet()) {
            // Publish inventory create event to Redis
            InventoryEventDto event = InventoryEventDto.createEvent(
                entry.getKey(),
                entry.getValue(),
                warehouseId
            );
            redisPublisher.publishInventoryEvent(event);
        }
    }
}
