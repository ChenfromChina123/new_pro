import type { CourseMeta, CoursePackage, Statement } from "@/types";

/**
 * 将数字转换为中文课程标题，如 1 -> "第一课"
 */
function convertToChineseTitle(num: number): string {
  const chineseNumbers = ["零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"];
  let title = "第";
  if (num >= 10) {
    const tens = Math.floor(num / 10);
    const ones = num % 10;
    if (tens !== 1) {
      title += chineseNumbers[tens];
    }
    title += "十";
    if (ones !== 0) {
      title += chineseNumbers[ones];
    }
  } else {
    title += chineseNumbers[num];
  }
  title += "课";
  return title;
}

/**
 * 使用 Vite 的 import.meta.glob 预加载所有课程 JSON 数据
 * 路径相对于本文件（src/data/），上 3 级到达 aispring 根目录，再进入 packages
 */
const courseModules = import.meta.glob<Statement[]>(
  "../../../packages/xingrong-courses/data/courses/*.json",
  { eager: true, import: "default" },
);

/**
 * 所有课程的元数据列表（共55课）
 */
export const courseMetas: CourseMeta[] = Object.entries(courseModules)
  .sort(([a], [b]) => {
    // 按文件名数字排序：01.json < 02.json < ...
    const numA = parseInt(a.match(/(\d+)\.json$/)?.[1] ?? "0", 10);
    const numB = parseInt(b.match(/(\d+)\.json$/)?.[1] ?? "0", 10);
    return numA - numB;
  })
  .map(([filePath, statements], sortedIndex) => {
    const match = filePath.match(/(\d+)\.json$/);
    const index = match ? parseInt(match[1], 10) : sortedIndex + 1;
    return {
      index,
      title: convertToChineseTitle(index),
      count: (statements as Statement[]).length,
      file: filePath,
    };
  });

/**
 * 所有课程包的定义列表
 * 当前将全部 55 课归入"星荣零基础学英语"这一个课程包
 * 如需扩展，在此数组中追加新的 CoursePackage 对象即可
 */
export const coursePackages: CoursePackage[] = [
  {
    id: "xingrong-beginner",
    name: "星荣零基础学英语",
    description: "从零开始，系统掌握日常英语词汇与基础句型，适合完全零基础的学习者。",
    icon: "🌟",
    level: "零基础",
    courses: courseMetas,
  },
];

/**
 * 根据课程包 id 获取对应的课程包，找不到则返回 undefined
 */
export function getPackageById(packageId: string): CoursePackage | undefined {
  return coursePackages.find((p) => p.id === packageId);
}

/**
 * 根据课程索引（1-55）获取该课程的全部题目数据
 */
export function getCourseStatements(courseIndex: number): Statement[] {
  const entry = Object.entries(courseModules).find(([filePath]) => {
    const match = filePath.match(/(\d+)\.json$/);
    return match && parseInt(match[1], 10) === courseIndex;
  });
  if (!entry) return [];
  return entry[1] as Statement[];
}
