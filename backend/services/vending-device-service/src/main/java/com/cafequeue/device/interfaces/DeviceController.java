package com.cafequeue.device.interfaces;

import com.cafequeue.common.contract.DeviceCommandRequest;
import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.device.application.DeviceApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceApplicationService deviceService;

    public DeviceController(DeviceApplicationService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<List<DeviceApplicationService.DeviceView>> list() {
        return ApiResponse.ok(deviceService.list());
    }

    @PatchMapping("/{deviceId}/status")
    public ApiResponse<DeviceApplicationService.DeviceView> updateStatus(@PathVariable String deviceId, @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(deviceService.updateStatus(deviceId, request.online(), request.waterTemperature()));
    }

    @PostMapping("/{deviceId}/commands")
    public ApiResponse<DeviceApplicationService.DeviceCommandView> command(@PathVariable String deviceId, @RequestBody DeviceCommandRequest request) {
        return ApiResponse.ok(deviceService.command(deviceId, request));
    }

    public record UpdateStatusRequest(boolean online, int waterTemperature) {
    }
}
