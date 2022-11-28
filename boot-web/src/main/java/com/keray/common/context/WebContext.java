package com.keray.common.context;

import com.keray.common.DeviceType;
import lombok.*;

/**
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebContext {

    /**
     * 客户端类型
     */
    private DeviceType deviceType;

    /**
     * 客户端uuid
     */
    private String uuid;

    /**
     * 客户端host
     */
    private String host;

    /**
     * 客户端ip
     */
    private String ip;

}
