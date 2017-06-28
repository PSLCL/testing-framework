test-s3bucket -bucketname dtf-staf-logging
#write-s3object -key /testId/templateId/runid/instanceid/dtfAws.log -file C:\var\opt\pslcl\dtf\log\dtfAws.log -bucketname dtf-staf-logging
write-s3object -keyprefix /testId/templateId/runid/instanceid -folder C:\var\opt\pslcl\dtf\log -bucketname dtf-staf-logging
