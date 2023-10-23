FROM localstack/localstack:2.2.0

COPY bootstrap.sh /etc/localstack/init/ready.d/bootstrap.sh
RUN chmod +x /etc/localstack/init/ready.d/bootstrap.sh
EXPOSE 4566
CMD ["/bin/bash","/etc/localstack/init/ready.d/bootstrap.sh"]