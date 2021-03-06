package org.apache.mesos.chronos.scheduler.jobs

import java.util.Date

import org.joda.time.{DateTime, Duration}

/*
 * Task status enum at Chronos level, don't care about
 * killed/lost state. Idle state is not valid, a TaskStat
 * should only be generated if a task is already running
 * or finished
 */
object ChronosTaskStatus extends Enumeration {
  type TaskStatus = Value
  val Success, Fail, Running, Idle = Value
}

class TaskStat (val taskId: String,
  val jobName: String,
  val taskSlaveId: String) {
  /*
   * Cassandra column names
   */
  val TASK_ID = "id"
  val JOB_NAME = "job_name"
  val TASK_EVENT_TIMESTAMP = "ts"
  val TASK_STATE = "task_state"
  val TASK_SLAVE_ID = "slave_id"

  //time stats
  var taskStartTs: Option[DateTime] = None
  var taskEndTs: Option[DateTime] = None
  var taskDuration: Option[Duration] = None

  var taskStatus: ChronosTaskStatus.Value = ChronosTaskStatus.Idle

  def getTaskRuntime(): Option[Duration] = {
    taskDuration
  }

  def setTaskStatus(status: ChronosTaskStatus.Value) = {
    //if already a terminal state, ignore
    if ((taskStatus != ChronosTaskStatus.Success) &&
        (taskStatus != ChronosTaskStatus.Fail)) {
      taskStatus = status
    }
  }

  def setTaskStartTs(startTs: Date) = {
    //update taskStartTs if new value is older
    val taskStartDatetime = new DateTime(startTs)
    taskStartTs = taskStartTs match {
      case Some(currTs: DateTime) =>
        if (taskStartDatetime.isBefore(currTs)) {
          Some(taskStartDatetime)
        } else {
          Some(currTs)
        }
      case None =>
        Some(taskStartDatetime)
    }

    taskEndTs match {
      case Some(ts) =>
        taskDuration = Some(new Duration(taskStartDatetime, ts))
      case None =>
    }
  }

  def setTaskEndTs(endTs: Date) = {
    val taskEndDatetime = new DateTime(endTs)
    taskEndTs = Some(taskEndDatetime)
    taskStartTs match {
      case Some(ts) =>
        taskDuration = Some(new Duration(ts, taskEndDatetime))
      case None =>
    }
  }

  override def toString(): String = {
    "taskId=%s job_name=%s slaveId=%s startTs=%s endTs=%s duration=%s status=%s".format(
      taskId, jobName, taskSlaveId,
      taskStartTs.toString(),
      taskEndTs.toString(),
      taskDuration.toString(),
      taskStatus.toString())
  }
}
