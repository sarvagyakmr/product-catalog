package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.OutwardOrder;
import com.example.ordermanagement.enums.Channel;
import com.example.ordermanagement.enums.OutwardOrderStatus;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutwardOrderRepository extends CrudRepository<OutwardOrder, Long> {
    Optional<OutwardOrder> findByChannelAndChannelOrderId(Channel channel, String channelOrderId);
    Iterable<OutwardOrder> findByStatus(OutwardOrderStatus status);
}
