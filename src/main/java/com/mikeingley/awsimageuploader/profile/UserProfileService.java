package com.mikeingley.awsimageuploader.profile;

import com.mikeingley.awsimageuploader.bucket.BucketName;
import com.mikeingley.awsimageuploader.filestore.FileStore;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class UserProfileService {

    private final UserProfileDataAccessService userProfileDataAccessService;
    private final FileStore fileStore;

    @Autowired
    public UserProfileService(UserProfileDataAccessService userProfileDataAccessService, FileStore fileStore){
        this.userProfileDataAccessService = userProfileDataAccessService;
        this.fileStore = fileStore;
    }

    List<UserProfile> getUserProfiles(){
        return userProfileDataAccessService.getUserProfiles();
    }

    public void uploadUserProfileImage(UUID userProfileId, MultipartFile file) {

        isFileEmpty(file);

        isImage(file);

        UserProfile user = getUserProfileOrThrow(userProfileId);

        Map<String, String> metadata = extractMetadata(file);

        String path = String.format("%s/%s", BucketName.PROFILE_IMAGE.getBucketName(), user.getUserProfileId());
        String filename = String.format("%s-%s", file.getOriginalFilename(), UUID.randomUUID());

        try {
            fileStore.save(path, filename, Optional.of(metadata), file.getInputStream());
            user.setUserProfileImageLink(filename);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    byte[] downloadUserProfileImage(UUID userProfileId){
        UserProfile user = getUserProfileOrThrow(userProfileId);
        String path = String.format("%s/%s",
                BucketName.PROFILE_IMAGE.getBucketName(),
                user.getUserProfileId());

        return user.getUserProfileImageLink()
                .map(key -> fileStore.download(path, key))
                .orElse(new byte[0]);
    }

    private Map<String, String> extractMetadata(MultipartFile file) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", file.getContentType());
        metadata.put("Content-Length", String.valueOf(file.getSize()));
        return metadata;
    }

    private UserProfile getUserProfileOrThrow(UUID userProfileId) {
        return userProfileDataAccessService
                .getUserProfiles()
                .stream()
                .filter(userProfile -> userProfile.getUserProfileId().equals(userProfileId))
                .findFirst()
                .orElseThrow(()-> new IllegalStateException(String.format("User profile %s not found", userProfileId)));
    }

    private void isImage(MultipartFile file) {
        if(!Arrays.asList(
                ContentType.IMAGE_JPEG.getMimeType(),
                ContentType.IMAGE_GIF.getMimeType(),
                ContentType.IMAGE_PNG.getMimeType()).contains(file.getContentType())){
            throw new IllegalStateException("invalid file type, must be jpeg/gif/png type tried was ->" + file.getContentType());
        }
    }

    private void isFileEmpty(MultipartFile file) {
        if(file.isEmpty()){
            throw new IllegalStateException("can't upload an empty file ["+file.getSize()+"]");
        }
    }


}
