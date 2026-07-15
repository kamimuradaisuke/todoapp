package com.todo.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.todo.app.entity.FileInfo;
import com.todo.app.entity.Todo;
import com.todo.app.mapper.FileMapper;
import com.todo.app.mapper.TodoMapper;

@Service
public class TodoService {

    @Autowired
    private TodoMapper todoMapper;

    @Autowired
    private FileMapper fileMapper;

    @Transactional
    public void addTodo(Todo todo, MultipartFile[] files) throws IOException {

        // Todo登録
        todoMapper.add(todo);

        // 添付ファイル保存
        saveFiles(todo.getTaskId(), files);
    }

    private final String uploadDir = "uploads";

    private void saveFiles(Long taskId, MultipartFile[] files) throws IOException {

        if (files == null || files.length == 0) {
            return;
        }

        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                continue;
            }

            String savedFileName =
                    UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path filePath = uploadPath.resolve(savedFileName);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setTaskId(taskId);
            fileInfo.setFileName(file.getOriginalFilename());
            fileInfo.setFilePath(filePath.toString());
            fileInfo.setFileSize(file.getSize());
            fileInfo.setContentType(file.getContentType());

            fileMapper.insertFile(fileInfo);
        }
    }
    @Transactional
    public void updateTodo(Todo todo, MultipartFile[] files) throws IOException {

        // タスク更新
        todoMapper.update(todo);

        // 添付ファイル保存
        saveFiles(todo.getTaskId(), files);

        // 親の状態に応じて子タスク更新
        if (todo.getDoneFlg() == 1) {
            todoMapper.doneSubTask(todo.getTaskId());
        } else {
            todoMapper.undoneSubTask(todo.getTaskId());
        }
    }
    @Transactional
    public void addSubTask(Todo todo, MultipartFile[] files) throws IOException {

        // サブタスク登録
        todoMapper.add(todo);

        // 添付ファイル保存
        saveFiles(todo.getTaskId(), files);
    }
    @Transactional
    public void updateSubTask(Todo todo, MultipartFile[] files) throws IOException {

        // サブタスク更新
        todoMapper.update(todo);

        // 添付ファイル保存
        saveFiles(todo.getTaskId(), files);
    }
    @Transactional
    public void uploadFiles(Long taskId, MultipartFile[] files) throws IOException {

        // 添付ファイルのみ追加
        saveFiles(taskId, files);
    }
}