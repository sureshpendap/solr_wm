/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.client.solrj.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SpeculativeExecutionStrategy {
  
  public int delay;
  
  public SpeculativeExecutionStrategy(int delay) {
    this.delay = delay;
  }
  
  public String execute() throws InterruptedException, ExecutionException {
      
    CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
      try {
        int rdelay = (int) (Math.random() * delay);
        Thread.sleep(rdelay);
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      }
         return "Good morning";
      });
      
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
          try {
            int rdelay = (int) (Math.random() * delay);
            Thread.sleep(rdelay);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
          if (!future1.isDone()) {
            return "Good afternoon";
          }
          throw new RuntimeException("did not trigger");
        
      });
        
     CompletableFuture<Object> result = CompletableFuture.anyOf(future1, future2);
     
     return (String) result.get();
  }
  
  
  public static void main(String[] args) {
    
    SpeculativeExecutionStrategy strategy = new SpeculativeExecutionStrategy(10);
    try {
      for (int i = 0; i < 100000; i++) {
        System.out.println(strategy.execute());
      }
     
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    
  }

}
