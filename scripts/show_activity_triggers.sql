-- Step 1: read the full text of the two triggers on extracurricular_activity.
-- Run each statement separately and copy the whole result (the grid truncates long text;
-- use "Results to Text" or copy the cell value).
EXEC sp_helptext N'dbo.TR_extracurricular_activity_approval_lifecycle';
EXEC sp_helptext N'dbo.TR_extracurricular_activity_business_rules';
