package com.keray.common.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author by keray
 * date:2021/4/22 5:31 下午
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadResult {
    private String path;
    private String taskId;
}
