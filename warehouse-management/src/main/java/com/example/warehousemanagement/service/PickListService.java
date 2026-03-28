package com.example.warehousemanagement.service;

import com.example.warehousemanagement.entity.BoxItem;
import com.example.warehousemanagement.entity.Item;
import com.example.warehousemanagement.entity.OutwardOrderItem;
import com.example.warehousemanagement.entity.PickList;
import com.example.warehousemanagement.entity.StorageBox;
import com.example.warehousemanagement.enums.ItemStatus;
import com.example.warehousemanagement.enums.PickListStatus;
import com.example.warehousemanagement.repository.BoxItemRepository;
import com.example.warehousemanagement.repository.ItemRepository;
import com.example.warehousemanagement.repository.OutwardOrderItemRepository;
import com.example.warehousemanagement.repository.PickListRepository;
import com.example.warehousemanagement.repository.StorageBoxRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickListService {

    private final PickListRepository pickListRepository;
    private final OutwardOrderItemRepository outwardOrderItemRepository;
    private final ItemRepository itemRepository;
    private final BoxItemRepository boxItemRepository;
    private final StorageBoxRepository storageBoxRepository;

    public PickListService(PickListRepository pickListRepository,
                           OutwardOrderItemRepository outwardOrderItemRepository,
                           ItemRepository itemRepository,
                           BoxItemRepository boxItemRepository,
                           StorageBoxRepository storageBoxRepository) {
        this.pickListRepository = pickListRepository;
        this.outwardOrderItemRepository = outwardOrderItemRepository;
        this.itemRepository = itemRepository;
        this.boxItemRepository = boxItemRepository;
        this.storageBoxRepository = storageBoxRepository;
    }

    @Transactional
    public void createPickLists() {
        // Get all order items where pickListCount < quantity
        List<OutwardOrderItem> allItems = (List<OutwardOrderItem>) outwardOrderItemRepository.findAll();
        
        for (OutwardOrderItem orderItem : allItems) {
            int pendingPickCount = orderItem.getQuantity() - orderItem.getPickListCount();
            if (pendingPickCount <= 0) {
                continue; // Already fully picked
            }

            Long productId = orderItem.getProductId();
            Long orderId = orderItem.getOrderId();

            // Check if there are LIVE items available for this product
            List<Item> liveItems = itemRepository.findByProductId(productId).stream()
                .filter(item -> item.getStatus() == ItemStatus.LIVE)
                .toList();

            if (liveItems.isEmpty()) {
                continue; // No LIVE items available
            }

            // Find a StorageBox that contains these LIVE items
            Long foundStorageBoxId = null;
            for (Item liveItem : liveItems) {
                List<BoxItem> boxItems = boxItemRepository.findByItemId(liveItem.getId());
                if (!boxItems.isEmpty()) {
                    Long candidateBoxId = boxItems.get(0).getBoxId();
                    // Verify the box exists
                    Optional<StorageBox> box = storageBoxRepository.findById(candidateBoxId);
                    if (box.isPresent()) {
                        foundStorageBoxId = candidateBoxId;
                        break;
                    }
                }
            }

            // Fallback: if no BoxItem association found, try to find any STORAGE type box
            if (foundStorageBoxId == null) {
                List<StorageBox> storageBoxes = storageBoxRepository.findByType(
                    com.example.warehousemanagement.enums.BoxType.STORAGE);
                if (!storageBoxes.isEmpty()) {
                    foundStorageBoxId = storageBoxes.get(0).getId();
                }
            }

            if (foundStorageBoxId == null) {
                continue; // No storage box found for LIVE items
            }

            // Check if pick list already exists for this order+product+box
            List<PickList> existing = pickListRepository.findByOrderIdAndProductId(orderId, productId);
            boolean alreadyHasPending = false;
            for (PickList pl : existing) {
                if (pl.getStorageBoxId().equals(foundStorageBoxId) && pl.getStatus() == PickListStatus.PENDING) {
                    alreadyHasPending = true;
                    break;
                }
            }

            if (alreadyHasPending) {
                continue; // Already have a pending pick list for this
            }

            // Create pick list
            PickList pickList = new PickList(orderId, productId, foundStorageBoxId, PickListStatus.PENDING);
            pickListRepository.save(pickList);

            // Increment pickListCount on order item
            orderItem.setPickListCount(orderItem.getPickListCount() + 1);
            outwardOrderItemRepository.save(orderItem);
        }
    }

    public Optional<PickList> getPickListById(Long id) {
        return pickListRepository.findById(id);
    }

    @Transactional
    public PickList pickItemForPickList(Long pickListId, Long itemId) {
        // Find the picklist
        PickList pickList = pickListRepository.findById(pickListId)
            .orElseThrow(() -> new IllegalArgumentException("PickList not found with id: " + pickListId));

        if (pickList.getStatus() != PickListStatus.PENDING) {
            throw new IllegalStateException("PickList is not in PENDING state, current state: " + pickList.getStatus());
        }

        Long orderId = pickList.getOrderId();
        Long productId = pickList.getProductId();

        // Find the specific item by ID
        Item pickedItem = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + itemId));

        // Verify item is LIVE
        if (pickedItem.getStatus() != ItemStatus.LIVE) {
            throw new IllegalStateException("Item is not in LIVE state, current state: " + pickedItem.getStatus());
        }

        // Mark item as PICKED
        pickedItem.setStatus(ItemStatus.PICKED);
        itemRepository.save(pickedItem);

        // Find the OutwardOrderItem and set the pickedItemId
        List<OutwardOrderItem> orderItems = outwardOrderItemRepository.findByOrderId(orderId);
        for (OutwardOrderItem orderItem : orderItems) {
            if (orderItem.getProductId().equals(productId) && orderItem.getPickedItemId() == null) {
                orderItem.setPickedItemId(pickedItem.getId());
                outwardOrderItemRepository.save(orderItem);
                break;
            }
        }

        // Update picklist status to PICKED
        pickList.setStatus(PickListStatus.PICKED);
        return pickListRepository.save(pickList);
    }

    @Transactional
    public void cancelPickListsForOrder(Long orderId) {
        // Find all picklists for this order
        List<PickList> pickLists = pickListRepository.findByOrderId(orderId);

        for (PickList pickList : pickLists) {
            if (pickList.getStatus() == PickListStatus.PICKED) {
                // Item was already picked - mark as REMOVED and delete order item association
                Long productId = pickList.getProductId();

                // Find the OutwardOrderItem with this productId and pickedItemId
                List<OutwardOrderItem> orderItems = outwardOrderItemRepository.findByOrderId(orderId);
                for (OutwardOrderItem orderItem : orderItems) {
                    if (orderItem.getProductId().equals(productId) && orderItem.getPickedItemId() != null) {
                        // Mark the item as REMOVED
                        Long pickedItemId = orderItem.getPickedItemId();
                        Optional<Item> itemOpt = itemRepository.findById(pickedItemId);
                        if (itemOpt.isPresent()) {
                            Item item = itemOpt.get();
                            item.setStatus(ItemStatus.REMOVED);
                            itemRepository.save(item);
                        }

                        // Delete the order item association (set pickedItemId to null)
                        orderItem.setPickedItemId(null);
                        outwardOrderItemRepository.save(orderItem);
                        break;
                    }
                }
            }

            // Update picklist status to CANCELLED
            pickList.setStatus(PickListStatus.CANCELLED);
            pickListRepository.save(pickList);
        }
    }

    @Transactional
    public void createPickListForItem(Long orderId, Long productId, Integer quantity) {
        // Find the order item
        List<OutwardOrderItem> orderItems = outwardOrderItemRepository.findByOrderId(orderId);
        OutwardOrderItem orderItem = orderItems.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst()
            .orElse(null);

        if (orderItem == null) {
            return; // Order item not found
        }

        int pendingPickCount = orderItem.getQuantity() - orderItem.getPickListCount();
        if (pendingPickCount <= 0) {
            return; // Already fully picked
        }

        // Check if there are LIVE items available for this product
        List<Item> liveItems = itemRepository.findByProductId(productId).stream()
            .filter(item -> item.getStatus() == ItemStatus.LIVE)
            .toList();

        if (liveItems.isEmpty()) {
            return; // No LIVE items available
        }

        // Find a StorageBox that contains these LIVE items
        Long foundStorageBoxId = null;
        for (Item liveItem : liveItems) {
            List<BoxItem> boxItems = boxItemRepository.findByItemId(liveItem.getId());
            if (!boxItems.isEmpty()) {
                Long candidateBoxId = boxItems.get(0).getBoxId();
                // Verify the box exists
                Optional<StorageBox> box = storageBoxRepository.findById(candidateBoxId);
                if (box.isPresent()) {
                    foundStorageBoxId = candidateBoxId;
                    break;
                }
            }
        }

        // Fallback: if no BoxItem association found, try to find any STORAGE type box
        if (foundStorageBoxId == null) {
            List<StorageBox> storageBoxes = storageBoxRepository.findByType(
                com.example.warehousemanagement.enums.BoxType.STORAGE);
            if (!storageBoxes.isEmpty()) {
                foundStorageBoxId = storageBoxes.get(0).getId();
            }
        }

        if (foundStorageBoxId == null) {
            return; // No storage box found for LIVE items
        }

        // Check if pick list already exists for this order+product+box
        List<PickList> existing = pickListRepository.findByOrderIdAndProductId(orderId, productId);
        boolean alreadyHasPending = false;
        for (PickList pl : existing) {
            if (pl.getStorageBoxId().equals(foundStorageBoxId) && pl.getStatus() == PickListStatus.PENDING) {
                alreadyHasPending = true;
                break;
            }
        }

        if (alreadyHasPending) {
            return; // Already have a pending pick list for this
        }

        // Create pick list
        PickList pickList = new PickList(orderId, productId, foundStorageBoxId, PickListStatus.PENDING);
        pickListRepository.save(pickList);

        // Increment pickListCount on order item
        orderItem.setPickListCount(orderItem.getPickListCount() + 1);
        outwardOrderItemRepository.save(orderItem);
    }
}
