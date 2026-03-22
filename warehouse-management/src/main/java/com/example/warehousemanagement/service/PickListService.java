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
}
