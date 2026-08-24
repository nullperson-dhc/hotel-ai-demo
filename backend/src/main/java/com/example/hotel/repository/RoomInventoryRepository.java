package com.example.hotel.repository;
import com.example.hotel.domain.RoomInventory; import java.time.*; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import org.springframework.transaction.annotation.Transactional;
public interface RoomInventoryRepository extends JpaRepository<RoomInventory,Long>{
 List<RoomInventory> findByRoomTypeIdAndBizDateGreaterThanEqualAndBizDateLessThan(Long roomTypeId,LocalDate from,LocalDate to);
 @Modifying @Transactional
 @Query("update RoomInventory i set i.availableStock=i.availableStock-:count,i.version=i.version+1,i.updatedAt=:now where i.roomType.id=:roomTypeId and i.bizDate=:date and i.availableStock>=:count")
 int decrement(@Param("roomTypeId")Long roomTypeId,@Param("date")LocalDate date,@Param("count")int count,@Param("now")OffsetDateTime now);
}
