#QA Portal

The QA Portal is composed of a REST API and a website which together allow users to view and create tests and test plans and 
control certain aspects of the Testing Framework.

##Use Cases

###A User Authenticates and System Determines Access

The user either authenticates to the system or continues anonymously. Anonymous access is limited to read-only access. 
Authenticated access is based on configuration, ideally through LDAP group membership. Ideally, all user information would 
be contained in LDAP.

###A User views QA reports.

Either anonymously or authenticated, the user may choose from a set of reports. Several of the reports will require prompts for 
additional information, which the user supplies. The report is viewed on the web site, including scrolling/paging through reports 
that may be very long.

###A User downloads QA reports as PDF.

Either anonymously or authenticated, a user views a QA report. An option allows the report (with whatever choices were used 
to generate it) to be downloaded as a PDF.

###An Authenticated User Creates, Edits, and Deletes Components.

An authenticated user manages the “components” table. This includes creating new entries, editing existing entries, and deleting 
entries. Before deletion, the user is warned about the impact (“This action will delete 5,000 test results.”).

###An Authenticated User Creates, Edits, and Deletes Test Plans.

An authenticated user manages the “test plan” table. This includes creating new entries, editing existing entries, and deleting 
entries. Before deletion, the user is warned about the impact (“This action will delete 50 tests and 5,000 test results.”).

###An Authenticated User Creates, Edits, and Deletes Tests.

An authenticated user manages the “test plan” table. This includes creating new entries, editing existing entries, and deleting 
entries. Before deletion, the user is warned about the impact (“This action will delete 50 tests and 5,000 test results.”).

##Future Use Cases

These features are not currently supported, but may be supported in a future version of the Testing Framework.

###A User Downloads QA Results as CSV.

Either anonymously or authenticated, a user views a QA report. An option allows the report (with whatever choices were used to 
generate it) to be downloaded as a CSV file.

###A User Monitors Currently Running Tests.

A user (anonymous or authenticated) views a dynamic list of currently running tests. Periodically the list is updated as tests 
complete (disappear from the list) or start (appear on the list). Appropriate meta-data from the database is shown in the report.

###A User Monitors Pending Tests.

A user (anonymous or authenticated) views a dynamic list of scheduled tests. Periodically the list is updated as tests start to 
run (removed from the list). Appropriate meta-data from the database is shown in the report. The view is able to list several 
thousand pending tests.

###A User Views a Dashboard of the System.

A user (anonymous or authenticated) views a dashboard of the system. The dashboard shows summary information from the database 
in the form of graphs.

###A User Monitors Current Performance.

A user (anonymous or authenticated) views a performance page of the system. The page shows historical performance information as 
a graph, and the graph periodically updates to show current information.
