package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.SlotResponseDTO;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:08-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class SlotMapperImpl implements SlotMapper {

    @Override
    public SlotResponseDTO toResponse(AvailabilitySlot slot) {
        if ( slot == null ) {
            return null;
        }

        Long id = null;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        String status = null;

        id = slot.getId();
        startTime = slot.getStartTime();
        endTime = slot.getEndTime();
        if ( slot.getStatus() != null ) {
            status = slot.getStatus().name();
        }

        SlotResponseDTO slotResponseDTO = new SlotResponseDTO( id, startTime, endTime, status );

        return slotResponseDTO;
    }

    @Override
    public List<SlotResponseDTO> toResponseList(List<AvailabilitySlot> slots) {
        if ( slots == null ) {
            return null;
        }

        List<SlotResponseDTO> list = new ArrayList<SlotResponseDTO>( slots.size() );
        for ( AvailabilitySlot availabilitySlot : slots ) {
            list.add( toResponse( availabilitySlot ) );
        }

        return list;
    }
}
