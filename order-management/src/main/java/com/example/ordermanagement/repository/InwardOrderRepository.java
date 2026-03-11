package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.InwardOrder;
import com.example.ordermanagement.enums.Channel;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InwardOrderRepository extends CrudRepository<InwardOrder, Long> {
    Optional<InwardOrder> findByChannelAndChannelOrderId(Channel channel, String channelOrderId);
}
