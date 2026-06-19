package com.todo.app.entity;

import java.time.LocalDate;

import lombok.Data;
@Data
public class Todo {

	private Long taskId;			// タスクID
	private String taskName;		//タスク名
	private Integer priorityId;		//優先度
	private Integer categoryId;		//カテゴリ
	private LocalDate scheduledWorkDate;		//実施予定日
	private Long parentId;			//親
	private String memo;   			//メモ
	private Integer doneFlg;
	
	
}

