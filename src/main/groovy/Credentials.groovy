
// Use https://github.com/jenkinsci/credentials-plugin to figure out package
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.CredentialsScope
import jenkins.model.Jenkins
import hudson.util.Secret
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl

// Based on https://youtu.be/RzWkn_ENVcc?t=2263
def domain = Domain.global()
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

def githubAccount = new UsernamePasswordCredentialsImpl(
        CredentialsScope.GLOBAL, "test-github", "Test Github Account",
        'testuser', // If using k8s secret: new File('/mnt/secrets/github/username').text,
        'testpassword', // If using k8s secret: new File('/mnt/secrets/github/token').text
)

def secretString = new StringCredentialsImpl(
        CredentialsScope.GLOBAL, "test-secret-string", "Test Secret String",
        Secret.fromString('testpassword') // If using k8s secret: Secret.fromString(new File('/mnt/secrets/github/token').text)
)

store.addCredentials(domain, githubAccount)
store.addCredentials(domain, secretString)

String keyfile = "/var/jenkins_home/.ssh/id_rsa"
// https://github.com/jenkinsci/ssh-credentials-plugin/blob/master/src/main/java/com/cloudbees/jenkins/plugins/sshcredentials/impl/BasicSSHUserPrivateKey.java#L103-L106
def privateKey = new BasicSSHUserPrivateKey(
        CredentialsScope.GLOBAL,
        "jenkins_ssh_key", // id
        "git", // username
        new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource(keyfile), // upload private key file
        "", // passphrase
        "") // description
store.addCredentials(domain, privateKey)

String minikubeKeyfile = "/var/jenkins_home/secret_data/minikube.pfx"
def minikubeCreds = new CertificateCredentialsImpl(
        CredentialsScope.GLOBAL,
        "minikube",
        "Minikube client certificate",
        "secret",
        new CertificateCredentialsImpl.FileOnMasterKeyStoreSource(minikubeKeyfile))
store.addCredentials(domain, minikubeCreds)