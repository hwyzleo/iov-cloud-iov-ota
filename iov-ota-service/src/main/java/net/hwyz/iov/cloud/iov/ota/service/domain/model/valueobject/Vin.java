package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import lombok.Getter;
import java.util.Objects;

@Getter
public class Vin {
    
    private final String value;
    
    public static Vin of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VIN must not be empty");
        }
        return new Vin(value);
    }
    
    private Vin(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vin vin = (Vin) o;
        return Objects.equals(value, vin.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}