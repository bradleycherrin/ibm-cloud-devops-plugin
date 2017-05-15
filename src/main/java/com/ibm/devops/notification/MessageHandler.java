/*
 <notice>

 Copyright 2017 IBM Corporation

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 </notice>
 */

package com.ibm.devops.notification;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.Job;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.PrintStream;
import com.ibm.devops.dra.Util;

//build message that will be posted to the webhook
public final class MessageHandler {
    public static JSONObject buildMessage(Run r, EnvVars envVars, String phase, String result){
        JSONObject message = new JSONObject();
        JSONObject build = new JSONObject();
        JSONObject scm = new JSONObject();

        Job job = r.getParent();
        String rootUrl = Jenkins.getInstance().getRootUrl();

        //setup scm
        if(envVars != null) {
            String gitCommit = envVars.get("GIT_COMMIT");
            String gitBranch = envVars.get("GIT_BRANCH");
            String gitPreviousCommit = envVars.get("GIT_PREVIOUS_COMMIT");
            String gitPreviousSuccessfulCommit = envVars.get("GIT_PREVIOUS_SUCCESSFUL_COMMIT");
            String gitUrl = envVars.get("GIT_URL");
            String gitCommitterName = envVars.get("GIT_COMMITTER_NAME");
            String gitCommitterEmail = envVars.get("GIT_COMMITTER_EMAIL");
            String gitAuthorName = envVars.get("GIT_AUTHOR_NAME");
            String gitAuthorEmail = envVars.get("GIT_AUTHOR_EMAIL");

            if (gitCommit != null) {
                scm.put("git_commit", gitCommit);
            }

            if (gitBranch != null) {
                scm.put("git_branch", gitBranch);
            }

            if (gitPreviousCommit != null) {
                scm.put("git_previous_commit", gitPreviousCommit);
            }

            if (gitPreviousSuccessfulCommit != null) {
                scm.put("git_previous_successful_commit", gitPreviousSuccessfulCommit);
            }

            if (gitUrl != null) {
                scm.put("git_url", gitUrl);
            }

            if (gitCommitterName != null) {
                scm.put("git_committer_name", gitCommitterName);
            }

            if (gitCommitterEmail != null) {
                scm.put("git_committer_email", gitCommitterEmail);
            }

            if (gitAuthorName != null) {
                scm.put("git_author_name", gitAuthorName);
            }

            if (gitAuthorEmail != null) {
                scm.put("git_author_email", gitAuthorEmail);
            }
        }

        //setup the build object
        build.put("number", r.getNumber());
        build.put("queue_id", r.getQueueId());
        build.put("phase", phase);
        build.put("url", r.getUrl());

        if(rootUrl != null){
            build.put("full_url", rootUrl + r.getUrl());
        } else{
            build.put("full_url", "");
        }

        if(result != null){
            build.put("status", result);
        }

        if(!"STARTED".equals(phase)) {
            build.put("duration", r.getDuration());
        }

        build.put("scm", scm);

        //setup the message
        message.put("name", job.getName());
        message.put("url", job.getUrl());
        message.put("build", build);

        return message;
    }

    //post message to webhook
    public static void postToWebhook(String webhook, JSONObject message, PrintStream printStream){
        //check webhook
        if(Util.isNullOrEmpty(webhook)){
            printStream.println("[IBM Cloud DevOps] IBM_CLOUD_DEVOPS_WEBHOOK_URL not set.");
            printStream.println("[IBM Cloud DevOps] Error: Failed to notify OTC.");
        } else {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost postMethod = new HttpPost(webhook);
            try {
                StringEntity data = new StringEntity(message.toString());
                postMethod.setEntity(data);
                postMethod = Proxy.addProxyInformation(postMethod);
                postMethod.addHeader("Content-Type", "application/json");
                CloseableHttpResponse response = httpClient.execute(postMethod);

                if (response.getStatusLine().toString().matches(".*2([0-9]{2}).*")) {
                    printStream.println("[IBM Cloud DevOps] Message successfully posted to webhook.");
                } else {
                    printStream.println("[IBM Cloud DevOps] Message failed, response status: " + response.getStatusLine());
                }
            } catch (IOException e) {
                printStream.println("[IBM Cloud DevOps] IOException, could not post to webhook:");
                e.printStackTrace(printStream);
            }
        }
    }
}