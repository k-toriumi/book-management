// 登録・編集ボタン押下時の処理
function postRequest(type, ope, url, target, json) {

  // モーダルの作成
  $('#modal_title').text('書籍の' + ope);
  $('#modal_message').text('「' + target + '」を' + ope + 'します。よろしいですか?');
  $('#modal_button2').text('キャンセル');
  $('#modal_button').text(ope);

  $('.mini.modal').modal({
    detachable : false,
    closable  : false,
    inverted: true,
    onDeny    : function(){},
    onApprove : function() {
      $.ajax({
          url: server_url + '/' + url,
          type:'POST',
          contentType:"application/json; charset=utf-8",
          dataType: 'json',
          data : JSON.stringify(json),
      }).done(function(data) {
        if(mode === 2) {
          // 編集モードの場合
          $.ajax({
            url: server_url + '/read/book/' + $('#book_id').val()
          }).done(function(res) {
            // 更新日時を最新化
            $('#book_update_date').val(res.book_update_date);
            $('#author_update_date').val(res.author_update_date);
          }).fail(function(XMLHttpRequest, textStatus, errorThrown) {});
        }
        success(ope);
      }).fail(function(XMLHttpRequest, textStatus, errorThrown) {
        setError(XMLHttpRequest, textStatus, errorThrown);
      });
    }
  })
  .modal('show');
}

// 削除ボタン押下時の処理
function del(id) {

  // 選択項目の取得
  var obj= getRowData(id);
  // DELETE POST
  postRequest(3, '削除', 'delete', obj['book_name'], {id : id, update_date : obj.book_update_date});
}

function setIncrementalSearch(area, box, type){

  // 検索ボックスの設定
  $(area)
    .search({
      apiSettings:  {
        url: server_url + '/read/author/0/{query}'
      },
      cache: false,
      onResultsClose: function() {
        if(type === 1) {
          now_page = 1;
          searchList('/read/' + now_page + '/' + encodeURIComponent($('#search_box').val()));
        } else {
          if($('#author_name').val() != "") {
            $.ajax({url: server_url + '/read/author/1/' + $('#author_name').val()})
              .done(function(res) {
                if(res.length == 1) {
                  $('#author_note').val(res[0].description);
                }
              }).fail(function(XMLHttpRequest, textStatus, errorThrown) {});
          }
        }
      }
    });

  // 検索条件のストリーム
  var queryStream = Rx.Observable.fromEvent($(box), 'input')
    .map(function(e) {
    return e.target.value;
  });

  // URLのストリーム
  var urlStream = queryStream
    .throttle(300)
    .distinctUntilChanged()
    .map(function(query) {
      return server_url + '/read/' + now_page + '/' + encodeURIComponent(query);
  });

  // ローディングのストリーム
  var loadingStream = new Rx.Subject();

  // 検索のストリーム
  var searchStream = urlStream
      .flatMap(function(query) {
          loadingStream.onNext(true);
          if(type === 1){
            now_page = 1;
          }
          return Rx.Observable.fromPromise($.ajax({url: query}))
              .finally(function() { loadingStream.onNext(false); });
      })
      .map(function(res) {
          return res;
      });

  // ローディングのサブスクライブ
  loadingStream.subscribe(function(loading) {
      if (loading) {
          $('#searchBox').addClass('loading');
      } else {
          $('#searchBox').removeClass('loading');
      }
  });

  // 検索のサブスクライブ
  searchStream.subscribe(function(res) {
    if(type === 1){
      // テーブル
      setTable(res);
      // ページャ
      setPager(res);
    } else {
      //
    }
  });
}

// 入力チェック
function check() {

    var check1 = requireCheck('#book_name', '書籍名');
    var check2 = requireCheck('#author_name', '著者');

    if(!check1 || !check2) {
      return false;
    }
    return true;
}

// 必須チェック
function requireCheck(item, name) {
  let field = item + '_field';
  let value = $(item).val();

  if(value === '') {
    // エラー
    $(field).addClass('field error');
    $(item).popup({content : name + 'を入力してください'});
    $(item).popup('show');
    return false;
  }
  // OK
  $(field).removeClass('error');
  $(item).popup({popup:false});
  $(item).popup('hide');
  return true;
}

function leaveOnlyNumber(e){
  // 数字以外の不要な文字を削除
  var st = String.fromCharCode(e.which);
  if ("0123456789".indexOf(st,0) < 0) { return false; }
  return true;
}

// テーブル設定処理
function setTable(res) {
  $('tbody').empty();
  if(res.total_count > 0){
    res.book_info.forEach(function(obj) {
      var contents = $('<tr>')
        .append('<td>' + obj.no +'</td>')
        .append('<td>' + obj.book_name + '</td>')
        .append('<td>' + obj.author_name + '</td>')
        .append('<td>' + obj.page + '</td>')
        .append('<td>' + obj.publisher + '</td>')
        .append('<td>' + obj.sale_date + '</td>')
        .append('<td>' + obj.isbn + '</td>')
        .append('<td>' + obj.book_note + '</td>')
        .append('<td onclick="edit(\'' + obj.book_id  + '\');"><i class="black edit icon"></i></td>')
        .append('<td onclick="del(\'' + obj.book_id + '\');"><i class="red times circle icon"></i></td>')
        .append('</tr>');
      $('tbody').append(contents);
    });
  }
  table_obj = res.book_info;
}

// ページャ設定処理
function setPager(res) {
  $('#pager').empty();

  var total_count = res.total_count;
  if(total_count > 0) {
    var page_count = parseInt((total_count - 1) / 10) + 1;
    if(now_page > 1){
        $('#pager').append('<a class="icon item" onclick="clickPage(' + (now_page - 1) + ');"><i class="left chevron icon"></i></a>');
    }

    if(page_count === 1){
      $('#pager').hide();
    } else {
      $('#pager').show();
      for(var i=1; i <= page_count; i++) {
          if (i === now_page) {
            $('#pager').append('<a class="item now_page" onclick="clickPage(' + i + ');">' + i + '</a>');
          } else {
            $('#pager').append('<a class="item" onclick="clickPage(' + i + ');">' + i + '</a>');
          }
      }
    }
    if(page_count > now_page){
      $('#pager').append('<a class="icon item" onclick="clickPage(' + (now_page + 1) + ');"><i class="right chevron icon"></i></a>');
    }

    let from = (now_page - 1) * 10 + 1;
    let to = now_page * 10 > total_count ? total_count : now_page * 10;
    $('#from_to_count').text(' ' + from + ' - ' + to);
    $('#total_count').text('全' + total_count + '件');
    $('#pager_area').show();
  } else {
    $('#pager_area').hide();
  }
}

// ページ遷移
function clickPage(page){

  now_page = page;
  searchList('/read/' + now_page + '/' + encodeURIComponent($('#search_box').val()));
}

// 一覧データ取得
function searchList(url){
  $.ajax({
    url: server_url + url
  }).done(function(res) {
    setTable(res);
    setPager(res);
  }).fail(function(XMLHttpRequest, textStatus, errorThrown) {
    setError(XMLHttpRequest, textStatus, errorThrown);
  });
}

// 完了処理
function success(ope) {

  // 処理後の検索
  searchList('/read/' + now_page + '/' + encodeURIComponent($('#search_box').val()));

  var color;
  switch(ope){
    case '登録':
      color = 'violet'
      break;
    case '更新':
      color = 'orange'
      break;
    case '削除':
      color = 'pink'
      break;
    default:
  }

  $('body')
    .toast({
    title: 'Completed',
    message: ope + 'が完了しました',
    displayTime: 5000,
    showIcon: 'check',
    class : color,
    className: {
      toast: 'ui message'
    }
  });
}

// エラー処理
function setError(XMLHttpRequest, textStatus, errorThrown) {

  if(XMLHttpRequest.status === 400){
    var errors = JSON.parse(XMLHttpRequest.responseText)._embedded.errors;
    errors.forEach(function(obj) {

      var messages = obj.message.split(':')

      var message = '';
      if (messages.length > 1) {
        message = messages[1];
      } else {
        message = obj.message;
      }
      $('body')
        .toast({
        title: 'Error',
        icon : 'window close outline',
        message: message,
        displayTime: 5000,
        class : 'red',
        className: {
          toast: 'ui message'
        }
      });
    });
  } else {
    $('body')
      .toast({
      title: 'Error',
      icon : 'window close outline',
      message: textStatus,
      displayTime: 5000,
      class : 'red',
      className: {
        toast: 'ui message'
      }
    });
  }
}

// 書籍登録ボタン押下時の処理
function create(id) {
  if(!right_area) {
    // 編集エリアが開いていない場合は開く
    toggleArea();
  }
  if (mode === 2) {
    $('#input_area').transition('horizontal flip', function() {

      // セグメントの色
      $('#input_segment').removeClass('yellow');
      $('#input_segment').addClass('blue');


      // タイトルとアイコン
      $('#input_title').empty();
      $('#input_title').append('<i class="plus square outline icon"></i>書籍を登録する');

      // ボタン
      $('#button1').text('登録');
      $('#button1').removeClass('yellow');
      $('#button1').addClass('blue');
      $('#button2').text('クリア');

      // フォームの入力内容を復元する
      setCreateForm();

      $('#input_area').transition('horizontal flip');
    });
    mode = 1;
    $('#create').hide();
  }
}

// 編集ボタン押下時の処理
function edit(id) {
  if(!right_area) {
    // 編集エリアが開いていない場合は開く
    toggleArea();
  }
  if (mode === 1) {
    // 編集
    $('#input_area').transition('horizontal flip', function() {

      // セグメントの色
      $('#input_segment').removeClass('blue');
      $('#input_segment').addClass('yellow');

      // タイトルとアイコン
      $('#input_title').empty();
      $('#input_title').append('<i class="edit icon"></i>書籍を編集する');

      // ボタン
      $('#button1').text('更新');
      $('#button1').removeClass('blue');
      $('#button1').addClass('yellow');
      $('#button2').text('戻す');

      // 登録フォームの内容を保存
      saveCreateForm();

      // フォームに値を設定する
      setEditForm(id)

      $('#input_area').transition('horizontal flip');
    });

    mode = 2;
    $('#create').show();
  } else {
      // フォームに値を設定する
      setEditForm(id)
  }
}

function setEditForm(id) {
  // テーブルからフォームへ値の設定
  var obj = getRowData(id);
  Object.keys(obj).forEach(function(item) {
    $('#' + item).val(obj[item]);
  });
}

// 表示・非表示切り替え処理
function toggleArea() {
  if(right_area) {
    // 非表示
    $('#input_area').transition('fly left',
      function(){
        $('#grid_area').removeClass('grid');
      }
    );
    $('#toggle_link').text('書籍を登録／編集する');
    right_area = false;
  } else {
    // 表示
    $('#grid_area').addClass('grid');
    $('#input_area').transition('fly left');
    $('#toggle_link').text('一覧のみ表示する');
    right_area = true;
  }
}

// クリア／戻すボタン押下処理
function preClear() {
  if(mode===2) {
    setEditForm($('#book_id').val());
    return false;
  }
  return true;
}

function getRowData(id) {
  // 行の取得
  var form = {}
  var obj = table_obj.find(obj => obj.book_id == id);
  form['book_id'] = obj.book_id;
  form['book_name'] = obj.book_name;
  form['page'] = obj.page;
  form['publisher'] = obj.publisher;
  form['isbn'] = obj.isbn;
  form['sale_date'] = obj.sale_date;
  form['book_note'] = obj.book_note;
  form['author_name'] = obj.author_name;
  form['author_note'] = obj.author_note;
  form['book_update_date'] = obj.book_update_date;
  form['author_update_date'] = obj.author_update_date;
  return form;
}

// 書籍を登録するフォーム欄保存処理
function saveCreateForm() {
  form_items.forEach(function(item) {
    create_form[item] = $('#' + item).val();
  });
}

// 書籍を登録するフォーム欄復元処理
function setCreateForm() {
  Object.keys(create_form).forEach(function(item) {
    $('#' + item).val(create_form[item]);
  });
}
