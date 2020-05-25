/** 
 * Copyright 2014 Netflix, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package rx.internal.operators;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.observables.ConnectableObservable;
import rx.observers.Subscribers;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;
/** 
 * Shares a single subscription to a source through a Subject.
 * @param < T > the source value type
 * @param < R > the result value type
 */
public final class OperatorMulticast<T,R> extends ConnectableObservable<R> {
  Observable<? extends T> source;
  Object guard;
  Func0<? extends Subject<? super T,? extends R>> subjectFactory;
  AtomicReference<Subject<? super T,? extends R>> connectedSubject;
  List<Subscriber<? super R>> waitingForConnect;
  /** 
 * Guarded by guard. 
 */
  public Subscriber<T> subscription;
  public Subscription guardedSubscription;
  public OperatorMulticast(  Observable<? extends T> source,  final Func0<? extends Subject<? super T,? extends R>> subjectFactory){
    this(new Object(),new AtomicReference<Subject<? super T,? extends R>>(),new ArrayList<Subscriber<? super R>>(),source,subjectFactory);
  }
  public OperatorMulticast(  final Object guard,  final AtomicReference<Subject<? super T,? extends R>> connectedSubject,  final List<Subscriber<? super R>> waitingForConnect,  Observable<? extends T> source,  final Func0<? extends Subject<? super T,? extends R>> subjectFactory){
    super(new OnSubscribe<R>(){
      @Override public void call(      Subscriber<? super R> subscriber){
synchronized (guard) {
          if (connectedSubject.get() == null) {
            waitingForConnect.add(subscriber);
          }
 else {
            connectedSubject.get().unsafeSubscribe(subscriber);
          }
        }
      }
    }
);
    this.guard=guard;
    this.connectedSubject=connectedSubject;
    this.waitingForConnect=waitingForConnect;
    this.source=source;
    this.subjectFactory=subjectFactory;
  }
  @Override public void connect(  Action1<? super Subscription> connection){
synchronized (guard) {
      if (subscription != null) {
        connection.call(guardedSubscription);
        return;
      }
 else {
        final Subject<? super T,? extends R> subject=subjectFactory.call();
        subscription=Subscribers.from(subject);
        final AtomicReference<Subscription> gs=new AtomicReference<Subscription>();
        gs.set(Subscriptions.create(new Action0(){
          @Override public void call(){
            Subscription s;
synchronized (guard) {
              if (guardedSubscription == gs.get()) {
                s=subscription;
                subscription=null;
                guardedSubscription=null;
                connectedSubject.set(null);
              }
 else               return;
            }
            if (s != null) {
              s.unsubscribe();
            }
          }
        }
));
        guardedSubscription=gs.get();
        for (        Subscriber<? super R> s : waitingForConnect) {
          subject.unsafeSubscribe(s);
        }
        waitingForConnect.clear();
        connectedSubject.set(subject);
      }
    }
    connection.call(guardedSubscription);
    Subscriber<T> sub;
synchronized (guard) {
      sub=subscription;
    }
    if (sub != null)     source.subscribe(sub);
  }
  public OperatorMulticast(){
  }
}
