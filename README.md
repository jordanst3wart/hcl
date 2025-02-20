## Hcl
A jdk library to parse, and write HCL variable files (or tfvar files). HCL is a configuration language used by HashiCorp tools like Terraform, Consul, Packer, and Vault. This does not support HCL variables, but just hcl files.

## Usage

### Parsing

```java
import bot.stewart.hcl.HclParser;
import java.util.Map;


String hcl = "region = {\n  default = \"us-west-1\"\n};
Map<String, Any> variables = Hcl.parse(hcl);
```

### Writing

```java
import bot.stewart.hcl.HclWriter;
import java.util.Map;

Map<String, Any> variables = new HashMap<>();
variables.put("region", Map.of("default", "us-west-1"));
String hcl = Hcl.write(variables);
```

### Support

- [x] Parsing HCL variables
- [x] Writing HCL variables
- [ ] Reading, and writing from POJOs
- [ ] streaming string inputs, and outputs (for large files)

Doesn't aim to look reading for_each loops, modules, or variable substitution. Ignores comments in `tfvars` files.

### Future
Reading, and writing from POJOs maybe supported in the future, like:

```java
import bot.stewart.hcl.HclWriter;
import bot.stewart.hcl.HclParser;
import java.util.Map;

class Region {
    private String default;

    public String getDefault() {
        return default;
    }

    public void setDefault(String default) {
        this.default = default;
    }
}

String hcl = "region = {\n  default = \"us-west-1\"\n};
Map<String, Region> parsedVariables = Hcl.marshal(hcl, Region.class);
```

### Spec
HCL spec (alhtough not formal) is here:
https://github.com/hashicorp/hcl/blob/main/hclsyntax/spec.md

### License
MIT license
