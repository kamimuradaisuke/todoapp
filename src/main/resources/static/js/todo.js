$(function() {
	console.log("todo.js 読み込み成功");

    //完了済みの個数取得・表示
    let doneCount = $('#donetodes').children("tr").length;
    $('#done_count').text(doneCount);

    //更新処理
    $('.todo input').change(function() {
        const todo = $(this).parents('.todo');
        const id = todo.find('input[name="id"]');
        const title = todo.find('input[name="title"]');
        const timeLimit = todo.find('input[name="time_limit"]');
        const isDone = todo.find('input[name="done_flg"]').prop("checked");
        let doneFlg;
        if (isDone == true) {
            doneFlg = 1;
        } else {
            doneFlg = 0;
        }

        const params = {
            id: id.val(),
            title: title.val(),
            time_limit: timeLimit.val(),
            done_flg: doneFlg
        }
        $.post("/update", params);

        //完了ボタンを押した際の処理
        doneCount = $('#done_count').text();

        if ($(this).prop('name') == "done_flg") {
            if (isDone == true) {
                $(todo).appendTo('#donetodes');
                todo.find('input[name="title"]').css('text-decoration', 'line-through')
                todo.find('input[name="time_limit"]').hide();
                doneCount++;
            } else {
                $(todo).appendTo('#todes');
                todo.find('input[name="title"]').css('text-decoration', 'none')
                todo.find('input[name="time_limit"]').show()
                doneCount--;
            }

            $("#done_count").text(doneCount);
        }


    })

    //完了済みタスク表示/非表示切り替え
    $('.button_for_show').click(function() {
        let showState = $('#done_table').css('display');
        if (showState == "none") {
            $('#done_table').show();
            $(this).css({ transform: ' rotate(225deg)', 'bottom': '-4px' });
        } else {
            $('#done_table').hide();
            $(this).css({ transform: ' rotate(45deg)', 'bottom': '4px' });
        }
    })


    //削除処理
    $('#delete').click(function() {
        $.post("/delete").done(function() {
            $('#donetodes').empty();
            $('#done_count').text(0);
        });
    });

    // 添付ファイル一覧表示
    const fileInput = document.getElementById("files");
    const fileList = document.getElementById("fileList");
    const fileCount = document.getElementById("fileCount");

    if (fileInput && fileList && fileCount) {

        console.log("ファイル選択のイベントを登録しました");

        fileInput.addEventListener("change", function() {

            console.log("change!");

            fileList.innerHTML = "";

            for (const file of this.files) {
                const li = document.createElement("li");
                li.textContent = "📄 " + file.name;
                fileList.appendChild(li);
            }

            fileCount.textContent = this.files.length;
        });

    }
    // 検索条件クリア
    $("#clearSearch").click(function() {
        $("input[name='taskName']").val("");
        $("input[name='fromDate']").val("");
        $("input[name='toDate']").val("");
        $("select[name='doneFlg']").prop("selectedIndex", 0);
        $("select[name='priorityId']").prop("selectedIndex", 0);
        $("select[name='categoryId']").prop("selectedIndex", 0);
    });
});
