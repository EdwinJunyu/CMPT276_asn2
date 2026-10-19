---
version: alpha
name: CS Staff Ratings
description: A simple teaching-staff rating site with readable HTML and basic CSS.
colors:
  primary: "#8f1c2c"
  ink: "#222222"
  muted: "#555555"
  background: "#f4f4f4"
  surface: "#ffffff"
  border: "#cccccc"
  link: "#244f86"
  error: "#a00020"
typography:
  body:
    fontFamily: "Arial, sans-serif"
rounded:
  DEFAULT: "4px"
spacing:
  page-max: "900px"
  page-padding: "24px"
components:
  button:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface}"
    padding: "10px 16px"
  panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.DEFAULT}"
  dialog:
    width: "420px"
---

# CS Staff Ratings 页面样式

## Overview

2026-10-18 按用户要求改为刚学前端的本科生容易阅读和维护的写法。
页面保留英文内容，代码关键位置使用中文注释。
业务仍按[项目蓝图](https://docs.google.com/document/d/1opvDkdFuhn2Vt8AxiC94u9Q7OtQ4Zu2Z4VAuDv9Tms4/edit)和当前 Controller / 实体执行。
本次移除 SVG、头像、进度条、装饰侧栏和动态评分字段循环，改为普通表格、逐项字段和单列布局。
这是用户明确要求的简化，不延续先前的三色评分视觉方案。

## Colors

运行时来源是 `src/main/resources/static/css/style.css`，本文件记录对应值。
为了方便初学者，CSS 直接写颜色，不使用自定义变量。
primary 对应 .button；ink/background 对应 body；surface/border 对应 header、.container、table；
muted 对应 .help-text；link 对应 a 和焦点轮廓；error 对应 .field-error 和 .input-invalid。
仅使用浅色主题；滚动条保留浏览器默认外观。

## Typography

所有文字使用 Arial 和 sans-serif 回退；正文 16px，h1 28px，h2 21px，辅助文字 14px。
不加载外部字体，不设置特殊字距或多套字体。长姓名、邮箱和评论可以换行。

## Layout

.container 最大宽度 900px，普通块级布局，表单字段竖向排列。
输入框最大宽度 520px，页面在 600px 以下只缩小外边距和内边距。
列表按业务要求全部显示，宽表格在 .table-scroll 内滚动；详情表格保持页面宽度。
页面自然滚动，不固定页面高度；没有侧栏、复杂网格或固定操作栏。

## Elevation & Depth

普通内容使用 1px 边框，不使用阴影。dialog 仅有基本边框和半透明背景。

## Shapes

按钮、输入框、页面容器和 dialog 都使用 4px 圆角；没有头像、图标或评分刻度。

## Components

五个现有 HTML 文件各自保留完整的 head、header、main、footer，方便直接阅读。
这是针对小型课程项目选择的少量静态重复，不使用参数化 fragment 或隐藏的图标注册区。
交互样式统一放在 style.css 中，提交和确认逻辑统一放在现有 app.js 中。
JavaScript 使用普通 function、for 循环和明确的 if 判断，避免箭头函数、数组链式操作、可选链和表单序列化。
表单明确写出 name、email、roleType、clarity、niceness、knowledgeableScore、comment 七个字段。
只保留必要的 th:each、th:field、th:if、th:errors 和链接绑定。
按钮保留 hover、focus、active、disabled 状态，提交中直接更新文字。
删除和离开脏表单继续使用浏览器原生 dialog，避免削弱已有确认功能。
其他交互约定见 [UX-CONTRACT.md](UX-CONTRACT.md)。

## Do's and Don'ts

- 保留一位小数平均分、两个角色称谓、UTC 时间、字段错误和输入回显。
- 用户内容通过 th:text / th:field 输出。
- 使用普通标签、class 选择器和少量基础布局，避免嵌套三元表达式、动态字段名、动画和伪元素装饰。
- 改善可读性不以移除校验、确认或测试为代价。
