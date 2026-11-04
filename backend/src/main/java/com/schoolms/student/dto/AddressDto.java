package com.schoolms.student.dto;

import com.schoolms.common.enums.AddressType;
import com.schoolms.student.StudentAddress;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressDto(
        AddressType addressType,
        @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @Size(max = 80) String state,
        @Pattern(regexp = "^$|^\\d{6}$", message = "validation.invalid") String pin
) {
    public static AddressDto from(StudentAddress address) {
        return new AddressDto(address.getAddressType(), address.getLine1(), address.getLine2(),
                address.getCity(), address.getDistrict(), address.getState(), address.getPin());
    }
}
