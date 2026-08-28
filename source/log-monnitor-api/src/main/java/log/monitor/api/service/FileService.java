package log.monitor.api.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import log.monitor.api.form.file.DeleteListFileForm;
import log.monitor.api.service.feign.FeignMediaService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class FileService {
    @Autowired
    private FeignMediaService feignMediaService;

    public void deleteFile(String filePath) {
        if (StringUtils.isNotBlank(filePath)) {
            handleDeleteMedia(new DeleteListFileForm(Collections.singletonList(filePath)));
        }
    }

    public void deleteFiles(List<String> filePaths) {
        if (filePaths != null && !filePaths.isEmpty()) {
            List<String> filesToDelete = new ArrayList<>();
            for (String filePath : filePaths) {
                if (StringUtils.isNotBlank(filePath)) {
                    filesToDelete.add(filePath);
                }
            }
            if (!filesToDelete.isEmpty()) {
                handleDeleteMedia(new DeleteListFileForm(filesToDelete));
            }
        }
    }

    private void handleDeleteMedia(DeleteListFileForm form) {
        try {
            feignMediaService.deleteListFile(form);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}