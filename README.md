# openam-auth-sample : Example of a custom authentication module in OpenAM


This project contains an example of a custom authentication module for each
major release of OpenAM.

In order to get the example for your OpenAM version, you need to switch to
the right GIT branch.

Note: If you choose to fork the project first, which we recommend,
replace `git@github.com:ForgeRock/openam-auth-sample.git` by your own fork URL.

### OpenAM 11.0.x

The branch for `11.0.x` is currently `11.0.0`.

```
$ git clone -b 11.0.0 -o forgerock git@github.com:ForgeRock/openam-auth-sample.git
```

You can also use the web application (github) to download the example:
[https://github.com/ForgeRock/openam-auth-sample/tree/11.0.0](https://github.com/ForgeRock/openam-auth-sample/tree/11.0.0)


### OpenAM 12.0.x

The branch for `12.0.x` is currently `12.0.0`.

```
$ git clone -b 12.0.0 -o forgerock git@github.com:ForgeRock/openam-auth-sample.git
```

You can also use the web application (github) to download the example:
[https://github.com/ForgeRock/openam-auth-sample/tree/12.0.0](https://github.com/ForgeRock/openam-auth-sample/tree/12.0.0)


### OpenAM 13.0.x -- Development branch

The branch for `13.0.x` is currently `trunk`.

```
$ git clone -b trunk -o forgerock git@github.com:ForgeRock/openam-auth-sample.git
```

You can also use the web application (github) to download the example:
[https://github.com/ForgeRock/openam-auth-sample/tree/trunk](https://github.com/ForgeRock/openam-auth-sample/tree/trunk)

* * *

# Working with GIT

Forking the repository instead of just downloading the source has some
advantages: you will be able to import ForgeRock changes easily by updating
your branch from the remote one.

We won't recommend to use GIT if you're not used to it. If you're not a GIT
user, downloading the example from the web application would be a good
alternative.

### Updating your branch

The custom authentication API doesn't change when we release a new version.
However, the pom.xml contains information that may required to be updated in
time, like the ForgeRock repository references.

If you have trouble to compile your module, please check first you are on the
 last version for your OpenAM version.

For doing so, you need to:
```
$ git fetch forgerock
$ git rebase forgerock/$REMOTE_BRANCH
```

*`$REMOTE_BRANCH` is the branch where you're based on. See the previous
section to know which remote branch is associated to your OpenAM version*

### Working on trunk

For people used to git, we will recommend to clone the project and create a
branch from the right version of OpenAM:

```
$ git clone -o forgerock git@github.com:ForgeRock/openam-auth-sample.git
$ git checkout -tb $YOUR_MODULE_NAME forgerock/$REMOTE_BRANCH
```

As trunk is the development branch, it is subject of
modification. For updating the trunk branch with the last changes, you need to:

```
$ git fetch forgerock
$ git rebase forgerock/trunk
```


* * *
This work is licensed under the Creative Commons
Attribution-NonCommercial-NoDerivs 3.0 Unported License.
To view a copy of this license, visit
<http://creativecommons.org/licenses/by-nc-nd/3.0/>
or send a letter to Creative Commons, 444 Castro Street,
Suite 900, Mountain View, California, 94041, USA.

Copyright 2013-2015 ForgeRock AS.