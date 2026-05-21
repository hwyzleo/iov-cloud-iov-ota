package net.hwyz.iov.cloud.iov.ota.service.domain.model.valueobject;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import java.util.Objects;

@Getter
public class UpgradeModeArg {
    
    private final JSONObject value;
    
    public static UpgradeModeArg fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return new UpgradeModeArg(JSONUtil.parseObj(json));
    }
    
    private UpgradeModeArg(JSONObject value) {
        this.value = value;
    }
    
    public String toJson() {
        return value != null ? value.toString() : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpgradeModeArg that = (UpgradeModeArg) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}