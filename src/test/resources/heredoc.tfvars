# String with heredoc syntax
user_data = <<-EOF
  #!/bin/bash
  echo "Hello, World!"
  yum update -y
  yum install -y httpd
  systemctl start httpd
  systemctl enable httpd
EOF
