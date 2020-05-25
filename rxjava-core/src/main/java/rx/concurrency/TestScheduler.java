/** 
 * Copyright 2013 Netflix, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package rx.concurrency;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func2;
public class TestScheduler extends Scheduler {
  public Queue<TimedAction<?>> queue=new PriorityQueue<TimedAction<?>>(11,new CompareActionsByTime());
public static class TimedAction<T> {
    public long time;
    public Func2<Scheduler,T,Subscription> action;
    public T state;
    public TestScheduler scheduler;
    public TimedAction(    TestScheduler scheduler,    long time,    Func2<Scheduler,T,Subscription> action,    T state){
      this.time=time;
      this.action=action;
      this.state=state;
      this.scheduler=scheduler;
    }
    @Override public String toString(){
      return String.format("TimedAction(time = %d, action = %s)",time,action.toString());
    }
    public TimedAction(){
    }
  }
public static class CompareActionsByTime implements Comparator<TimedAction<?>> {
    @Override public int compare(    TimedAction<?> action1,    TimedAction<?> action2){
      return Long.valueOf(action1.time).compareTo(Long.valueOf(action2.time));
    }
    public CompareActionsByTime(){
    }
  }
  public long time;
  @Override public long now(){
    return TimeUnit.NANOSECONDS.toMillis(time);
  }
  public void advanceTimeBy(  long delayTime,  TimeUnit unit){
    advanceTimeTo(time + unit.toNanos(delayTime),TimeUnit.NANOSECONDS);
  }
  public void advanceTimeTo(  long delayTime,  TimeUnit unit){
    long targetTime=unit.toNanos(delayTime);
    triggerActions(targetTime);
  }
  public void triggerActions(){
    triggerActions(time);
  }
  @SuppressWarnings("unchecked") public void triggerActions(  long targetTimeInNanos){
    while (!queue.isEmpty()) {
      TimedAction<?> current=queue.peek();
      if (current.time > targetTimeInNanos) {
        time=targetTimeInNanos;
        break;
      }
      time=current.time;
      queue.remove();
      ((Func2<Scheduler,Object,Subscription>)current.action).call(current.scheduler,current.state);
    }
  }
  @Override public <T>Subscription schedule(  T state,  Func2<Scheduler,T,Subscription> action){
    return schedule(state,action,0,TimeUnit.MILLISECONDS);
  }
  @Override public <T>Subscription schedule(  T state,  Func2<Scheduler,T,Subscription> action,  long delayTime,  TimeUnit unit){
    queue.add(new TimedAction<T>(this,time + unit.toNanos(delayTime),action,state));
    return Subscriptions.empty();
  }
  public TestScheduler(){
  }
}
