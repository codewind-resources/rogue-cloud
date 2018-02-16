<%@ page import="java.util.List, com.roguecloud.server.RsDatabase.*"%>

<!DOCTYPE html>
<%
	DatabasePage dbPage = (DatabasePage) request.getAttribute("page");
%>
<html>
<title><%=dbPage.getTitle()%></title>
<header>
	<style>
body {
	font-family: "Arial", Courier, monospace ;
}

h1 {
	font-size:  24pt;
	font-variant:small-caps;
}

div#container-inner {
	background: none;
	color: black;
	padding: 5px;
	border: none;
	text-align: center;
	z-index: 10;	
}


table#main-table {
	width: 896px;
	border-collapse: collapse;
	margin-left: auto;
	margin-right: auto;
}
table#main-table th {
	font-weight: bold;
	font-size: 14pt;
	color: black;
}
table#main-table td {
	border-top: 1px solid black;
}
table#main-table tr.dark td {
	background: #0877BC;
	color: white;
}
table#main-table tr.light td {
	background: white;
	color: black;
}
table#main-table tr.searched {
	border: 4px solid red;
}

a:link, a:visited {
    color: inherit;
}

</style>

</header>		

<body>

	<div style="text-align: center;">
		<h1><%=dbPage.getTitle()%></h1>
	</div>

<% boolean isDark = false; %>

<div id="container-inner">
	<table id="main-table">
	<tbody>
		<tr class="dark">
			<%
				List<String> header = dbPage.getEntries().get(0);
				for (int x = 0; x < header.size(); x++) {
			%>
			<th><%=header.get(x)%></th>
			<%
				}
			%>
		</tr>

		<%
			for (int x = 1; x < dbPage.getEntries().size(); x++) {
				List<String> cols = dbPage.getEntries().get(x);
		%>
		<tr class="<%= (isDark ? "dark" : "light"  ) %>">
			<%
			for (int col = 0; col < cols.size(); col++) {
			%>
				<td><%=  cols.get(col) %></td>
			<%
			}
			
			isDark = !isDark;
			%>
		</tr>

		<%
			}
		%>

	</tbody>
	</table>
	
	</div>

</body>
</html>

