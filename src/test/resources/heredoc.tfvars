user_data2 = <<FOO
#!/bin/bash
echo "Hello, World!"
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
FOO

# TODO: This is not supported by the parser
# String with heredoc syntax
user_data = <<-EOF
  #!/bin/bash
  echo "Hello, World!"
  yum update -y
  yum install -y httpd
  systemctl start httpd
  systemctl enable httpd
EOF
