package com.todo.app.entity;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class Todo {

    private Long taskId;            // タスクID

    @NotBlank(message = "タスク名は必須です")
    private String taskName;        // タスク名

    private Integer priorityId;     // 優先度
    private Integer categoryId;     // カテゴリ
    private LocalDate scheduledWorkDate; // 実施予定日
    private Long parentId;          // 親タスク
    private String memo;            // メモ
    private Integer doneFlg;        // 完了フラグ

    private List<Todo> subTasks;    // サブタスク
}