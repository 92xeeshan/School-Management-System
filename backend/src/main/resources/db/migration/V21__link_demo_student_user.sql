SET app.bypass_rls = 'true';

UPDATE student
SET user_id = (SELECT id FROM app_user WHERE username = 'student' LIMIT 1)
WHERE admission_no = 'ADM0001'
  AND user_id IS NULL
  AND EXISTS (SELECT 1 FROM app_user WHERE username = 'student');
