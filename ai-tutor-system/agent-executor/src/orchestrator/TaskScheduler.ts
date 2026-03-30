/**
 * 任务调度器
 */
export class TaskScheduler {
  private queue: Array<{
    taskId: string;
    input: string;
    sessionId: string;
    priority: number;
  }> = [];
  
  private running: Map<string, boolean> = new Map();
  private maxConcurrent = 1;
  
  /**
   * 添加任务到队列
   */
  enqueue(taskId: string, input: string, sessionId: string, priority = 0): void {
    this.queue.push({ taskId, input, sessionId, priority });
    this.queue.sort((a, b) => b.priority - a.priority);
  }
  
  /**
   * 获取下一个任务
   */
  dequeue(): { taskId: string; input: string; sessionId: string } | null {
    if (this.running.size >= this.maxConcurrent) {
      return null;
    }
    
    const task = this.queue.shift();
    if (task) {
      this.running.set(task.taskId, true);
    }
    return task || null;
  }
  
  /**
   * 标记任务完成
   */
  complete(taskId: string): void {
    this.running.delete(taskId);
  }
  
  /**
   * 获取队列长度
   */
  getQueueLength(): number {
    return this.queue.length;
  }
  
  /**
   * 获取运行中的任务数
   */
  getRunningCount(): number {
    return this.running.size;
  }
  
  /**
   * 检查是否为空
   */
  isEmpty(): boolean {
    return this.queue.length === 0 && this.running.size === 0;
  }
}
