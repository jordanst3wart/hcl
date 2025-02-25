## Hcl
A jdk library to parse, and write HCL variable files (or tfvar files). HCL is a configuration language used by HashiCorp tools like Terraform, Consul, Packer, and Vault. This does not support HCL variables, but just hcl variable files. ie. no support for resource blocks

## Installation

**build.gradle.kts**
```kotlin
implementation("bot.stewart:hcl:0.2.0")
```

**build.gradle**
```groovy
implementation 'bot.stewart:hcl:0.2.0'
```

**maven pom**
```xml
<dependency>
    <groupId>bot.stewart</groupId>
    <artifactId>hcl</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Usage

### Parsing

```java
String hcl = "region = {\n  default = \"us-west-1\"\n};
HclParser parser = new HclParser();
Map<String, Any> variables = parser.parse(hcl);
```

### Writing

```java
Map<String, Any> variables = new HashMap<>();
variables.put("region", Map.of("default", "us-west-1"));
String hcl = Hcl.write(variables);
```

### Support

- [x] Parsing HCL variables (except [indented heredoc strings](https://developer.hashicorp.com/terraform/language/expressions/strings#indented-heredocs))
- [x] Writing HCL variables
- [x] Writing from POJOs to tfvars
- [ ] Marshalling to POJOs
- [ ] streaming string inputs, and outputs (for large files)

Doesn't aim to look reading for_each loops, modules, or variable substitution.

### Spec
The HCL spec (alhtough not formal) is here:
https://github.com/hashicorp/hcl/blob/main/hclsyntax/spec.md

### License
MIT license

### Miscellanous
If you don't want to add a dependency this library is currently just two kotlin files. You can copy them into your project. This library has no dependencies. 
