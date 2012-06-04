# Amazon EC2 Windows example
This example demonstrates how to start a Windows Server instance on Amazon
EC2 and obtain its randomly-generated Administrator password. Once you have
this, you can log in to the server via RDP.

## Usage
After building the example with `mvn assembly:assembly`, you can launch it like
this:

```
java -jar target/ec2-windows-jar-with-dependencies.jar --identity $AWS_API_KEY --credential $AWS_SECRET_KEY <optional arguments>
```

Replace `$AWS_API_KEY` and `$AWS_SECRET_KEY` appropriately.

The following optional arguments are recognized:

  * `--region <regionname>` - specify the EC2 region name to launch in
  * `--instance-type <instancetype>` - specify the EC2 instance type - defaults
    to `m1.small`
  * `--image-pattern <pattern>` - specify the pattern to select the image -
    this defaults to a pattern that will match the base, English, version of
    the current (or recent) Windows Server release.

The example will start the Windows instance, wait for the encrypted password
to become available, and then decrypt it. It will display the public IP
address, user name (which is always Administrator) and password - you can
provide these to the Remote Desktop client and log in to the new instance.

Once the instance is started, the example will wait for you to hit Enter on
the command line. After hitting Enter, the new instance will be shut down.
