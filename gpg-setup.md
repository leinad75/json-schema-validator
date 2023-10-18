How to create and use a GPG key
-----

gpg --gen-key
gpg --list-keys
gpg --keyserver keyserver.ubuntu.com --send-keys <key-id>
gpg --armor --export-secret-keys <key-id>

See https://dzone.com/articles/how-to-publish-artifacts-to-maven-central
