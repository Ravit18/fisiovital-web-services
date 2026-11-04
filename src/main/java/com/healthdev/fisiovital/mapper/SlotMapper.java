package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.SlotResponseDTO;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface SlotMapper {

    SlotResponseDTO toResponse(AvailabilitySlot slot);

    List<SlotResponseDTO> toResponseList(List<AvailabilitySlot> slots);
}
