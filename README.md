## Tfvars
A jdk library to parse, and write HCL variable files (does not have variable support). HCL is a configuration language used by HashiCorp tools like Terraform, Consul, Packer, and Vault.

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