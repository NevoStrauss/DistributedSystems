<h1>Distributed Systems Assignment 1</h1>

<h3>By</h3>
<ul>
<li>Eran Krauss 206337396</li>
<li>Nevo Strauss 311177638</li>
</ul>

<div> 
<h3>Content</h3>
<ol>
<li><a href="#po"> Project overview </a></li>
<li><a href="##run"> Running the project </a></li>
<li><a href="##mech"> Mechanism </a></li>
<li><a href="##add"> Additional notes</a></li>
</ol>
</div>

<div id="#po">
<h3 id="po">Project overview</h3>
<ul>
<li>Program input: text file with pdf urls and commands, <a href="input.txt">input example.</a></li>
<li>Program output: HTML file with processed pdf urls, <a href="htmlSummary.html">output example.</a></li>
<li>The program uses an aws computing and storage resources to execute a conversion on each pdf file:</li>
<ul>
<li>Computing: EC2 instances</li>
<li>Storage: S3 buckets </li>
<li>Messaging and communication: SQS (AWS Simple Queue Service) </li>
</ul>
<li>On each input file, the programs runs several ec2 instances (workers) to Asynchronously executing the pdf conversion</li>
</ul>
</div>

<div id="#run">
<h3>Running the project</h3>
<h4>Prerequisites:</h4>
<ul>
<li>Java 8, install with:</li>
<p>On Linux/Mac: <code> sudo apt install java-1.8.0-openjdk</code>
<p>On Windows: follow installation here <a href="https://openjdk.java.net/install/">jdk8 for windows</a></p>
</ul>
</div>

<div id="#mech">
<h3>Mechanism</h3>
<ul>
<li><a href="https://miro.com/welcomeonboard/YUpNSUM5U2pIQVpqcHBTSENEYVBjS2VsbGdTWWs4RU14cjA3SGo4eXMzakZ0Z3Bab0xSVXhQUEU3TnozRmNqenwzNDU4NzY0NTE0MTYyMTU3MzYy?invite_link_id=826418044872">See</a> program flow on Miro</li>
<li><h4>Main steps:</h4></li>
<ul>
<li><h5>Local app:</h5></li>
</ul>
<ol>
<li>Some local app has started a session</li>
<li>Local app starts or creates an EC2 Manager instance</li>
<li>Local app uploads the input file to some S3 bucket</li>
<li>Local app sends the input details to the manager</li>
<li>Local app waits for the manager to send "task_completed"</li>
<li>Local app terminates Manager</li>
<li>While receiving, gets the summary file from S3 output bucket, and run it on the browser</li>
</ol>

<ul>
<li><h5>Manager and Workers:</h5></li>
</ul>
<ol>
<li>Manager receives the input file location on S3 from Local app</li>
<li>Manager parse input file</li>
<li>Manager creates workers (number depends on input file size)</li>
<li>Manager sends each line in the file to SQS </li>
<li>Workers read lines from SQS and convert pdf files asynchronously</li>
<li>Workers upload converted pdf to S3 bucket </li>
<li>Workers sends 'task_done' message to Manager on each pdf</li>
<li>Manager validates that all the pdf files handled by the workers</li>
<li>Manager creates summary file, and upload it to S3 output bucket</li>
<li>Manager terminates workers</li>
<li>Manger sends 'task_completed' to local app</li>
</ol>



</ul>
</div>



<div id="#add">
<h3>Additional notes</h3>
<ul>
<li>Github repository<a href="https://github.com/NevoStrauss/DistributedSystems"> here</a></li>
<li>Contact us with:</li>
<ul>
<li>nevos@wix.com</li>
<li>erankr@wix.com</li>
</ul>
</ul>
</div>
