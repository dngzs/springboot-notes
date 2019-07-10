<%--
  Created by IntelliJ IDEA.
  User: BG317957
  Date: 2019/5/14
  Time: 19:48
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Insert title here</title>
</head>
<body>
<form name="serForm" action="/upload1" method="post"  enctype="multipart/form-data">
    <h1>采用流的方式上传文件</h1>
    <input type="file" name="file">
    <input type="submit" value="upload"/>
</form>

<form name="Form2" action="/upload2" method="post"  enctype="multipart/form-data">
    <h1>采用multipart提供的file.transfer方法上传文件</h1>
    <input type="file" name="file">
    <input type="submit" value="upload"/>
</form>

</body>
</html>
