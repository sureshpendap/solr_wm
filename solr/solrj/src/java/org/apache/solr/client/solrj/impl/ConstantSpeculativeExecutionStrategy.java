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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.util.NamedList;

public class ConstantSpeculativeExecutionStrategy implements RequestExecutionStrategy {
  
  private int delay;
  
  public ConstantSpeculativeExecutionStrategy(int delay) {
    this.delay = delay;
  }
  
  public NamedList<Object> execute(SolrClient client, SolrRequest req, SolrClient fallbackClient, SolrRequest fallbackRequest) throws SolrServerException, IOException, InterruptedException, ExecutionException {
    CompletableFuture<NamedList<Object>> primResp = CompletableFuture.supplyAsync(() -> {
        try {
          return client.request(req, null);
        } catch (SolrServerException | IOException e) {
          throw new RuntimeException(e);
        }
    });
    
    CompletableFuture<NamedList<Object>> secResp = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (!primResp.isDone()) {
        try {
          return fallbackClient.request(req, null);
        } catch (SolrServerException | IOException e) {
          throw new RuntimeException(e);
        }
      }
      throw new RuntimeException("Speculative execution did not get triggered");
  });
    
    CompletableFuture<Object> result = CompletableFuture.anyOf(primResp, secResp);
    
    return (NamedList<Object>)  result.get();
   
  }

}
