/**
 * 题目（语句）类型：一个英文句子及其中文翻译和音标
 */
export interface Statement {
  /** 中文翻译 */
  chinese: string;
  /** 英文内容 */
  english: string;
  /** 音标 */
  soundmark: string;
}

/**
 * 课程元数据：用于课程列表展示
 */
export interface CourseMeta {
  /** 课程序号（1-55） */
  index: number;
  /** 课程标题，如"第一课" */
  title: string;
  /** 题目总数 */
  count: number;
  /** JSON 文件路径（动态import用） */
  file: string;
}

/**
 * 已加载的完整课程数据
 */
export interface Course extends CourseMeta {
  /** 所有题目 */
  statements: Statement[];
}

/**
 * 课程包：包含若干课程的合集
 */
export interface CoursePackage {
  /** 唯一标识符 */
  id: string;
  /** 包名称 */
  name: string;
  /** 简短描述 */
  description: string;
  /** 展示图标（emoji） */
  icon: string;
  /** 难度标签，如"零基础"、"初级"等 */
  level: string;
  /** 包含的课程元数据列表 */
  courses: CourseMeta[];
}

/**
 * 打字输入中的单词状态
 */
export interface InputWord {
  /** 单词原文 */
  text: string;
  /** 是否是当前激活的单词 */
  isActive: boolean;
  /** 用户已输入的内容 */
  userInput: string;
  /** 是否输入错误 */
  incorrect: boolean;
  /** 在 inputValue 中的起始位置 */
  start: number;
  /** 在 inputValue 中的结束位置 */
  end: number;
  /** 唯一 id（即该单词在句子 split 后的索引） */
  id: number;
}
