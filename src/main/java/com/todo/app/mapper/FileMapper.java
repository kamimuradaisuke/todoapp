package com.todo.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.todo.app.entity.FileInfo;

@Mapper
public interface FileMapper {

    void insertFile(FileInfo file);

    List<FileInfo> selectFileList(Long taskId);
    List<FileInfo> selectByTaskId(Long taskId);
    FileInfo selectByFileId(Long fileId);
    void deleteFile(Long fileId);
    void deleteCompleteFiles();
    
}