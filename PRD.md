论文自动分类与代码关联系统 PRD
一、产品概述
1.1 产品名称

论文自动分类与代码关联系统（Paper Classification & Code Linking System）

1.2 产品目标

构建一个面向推荐系统研究的论文管理工具，实现：

自动分类论文（基于 LLM）
支持多标签分类
论文与代码链接关联
支持搜索、编辑、删除
提供高效浏览与跳转能力
二、核心功能
2.1 论文上传与自动分类
功能描述

用户上传 PDF，系统自动解析 Abstract + Introduction，并调用 LLM 完成分类。

分类类别（固定枚举）
CF Based 基于 CF
Graph Based 基于图的
Context Based 基于情境
Hybrid Based 混合动力
LLM Based 基于 LLM

分类规则
一个论文可以属于 多个类别（Multi-label）
流程
上传 PDF → 解析文本 → 提取 Abstract + Introduction → 调用 LLM → 返回多个分类 → 入库
LLM Prompt（建议版本）
你是推荐系统领域专家，请根据论文内容判断其属于以下哪些类别（可以多选）：

1. CF Based
2. Graph Based
3. Context Based
4. Hybrid Based
5. LLM Based

返回格式：
["CF Based", "Graph Based"]

论文内容如下：
{abstract + introduction}

2.2 PDF 解析
功能描述

从 PDF 中提取：

Abstract摘要
Introduction简介
技术建议
Tika

2.3 分类展示界面
页面结构
CF Based：
  paper1.pdf      code_url
  paper2.pdf      code_url

Graph Based：
  paper1.pdf      code_url
  paper3.pdf      code_url
功能要求
按分类分组展示
支持一个论文在多个分类下重复展示
每行包含：
PDF 名称（可点击）
code_url（可点击）
交互行为
元素	行为
PDF 名	打开静态文件 URL
code_url	跳转外部链接
2.4 论文与代码关联
功能描述
code_url 由用户 手动填写
数据字段
字段	说明
id身份证	主键
title标题	文件名
file_path	PDF路径
categories类别	多标签
code_url	代码链接
created_at	创建时间
2.5 编辑功能
功能描述

支持对论文进行编辑：

修改分类
修改 code_url
2.6 删除功能
功能描述
删除论文记录
同时删除：
数据库记录
PDF文件

2.7 搜索功能
功能描述

支持搜索论文：
搜索维度
文件名
分类
示例
搜索关键词：GNN
→ 返回包含 GNN 的论文

2.8 登录功能
功能描述
仅支持登录
不支持注册
无权限控制
实现方式
简单用户表

三、系统流程
用户流程
登录
 ↓
进入系统
 ↓
上传论文
 ↓
自动分类
 ↓
填写 code_url
 ↓
浏览分类
 ↓
搜索 / 编辑 / 删除

四、系统架构设计
4.1 技术选型
层级	技术
前端	React
后端	Java（Spring Boot）
数据库	MySQL
文件存储 OSS
PDF解析	PDFBox
LLM调用	Qwen-plus

4.2 架构图（逻辑）
前端
 ↓
Spring Boot 后端
 ├── 文件上传
 ├── PDF解析
 ├── LLM分类
 ├── CRUD接口
 ↓
MySQL + 文件存储
五、数据库设计（重点）
5.1 paper 表5.1 纸表
CREATE TABLE paper (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  file_path VARCHAR(500),
  code_url VARCHAR(500),
  created_at DATETIME
);
5.2 category 表（枚举表）
CREATE TABLE category (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50)
);
5.3 paper_category（多对多关系）
CREATE TABLE paper_category (
  paper_id BIGINT,
  category_id INT,
  PRIMARY KEY (paper_id, category_id)
);

六、接口设计
6.1 登录
POST /api/login
6.2 上传论文
POST /api/papers/upload

返回：
{
  "paperId": 1,
  "categories": ["Graph Based", "CF Based"]
}
6.3 获取论文列表
GET /api/papers

支持参数：

keyword关键词
category类别
6.4 更新论文
PUT /api/papers/{id}
6.5 删除论文
DELETE /api/papers/{id}
七、关键设计点
7.1 多标签分类
一个论文 → 多 category
前端展示：重复出现在多个分类
7.2 PDF访问
使用静态 URL（如 /files/xxx.pdf）
Spring Boot 配置静态资源映射
7.3 搜索实现

建议：
WHERE title LIKE '%keyword%'
category join类别连接


7.4 LLM调用策略

优化建议：

仅发送：
abstract
introduction
控制 token 成本

八、非功能需求
8.1 性能
单次分类 < 10s
支持小规模并发（<5用户）
8.2 可扩展性
