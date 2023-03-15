#Security + mTLS RH Service Mesh Demo

The Goal is to demonstrate Several options to grant access to a given workload, and to Encrypt Pod to Pod Channel using mutual TLS.  

## mTLS - Show Permissive , Strict and Disable modes.

1. By Default, Istio Enables auto mTLS on its managed workloads, and it accepts both TLS encrypted  and plain text traffics (Called Permissive mode).
   Let Disable Permissive mode by creating a `PeerAuthentication` that disable it.
```yaml
kind: PeerAuthentication
apiVersion: security.istio.io/v1beta1
metadata:
  name: strict-policy
  namespace: demo
  labels:
    demoMeshResource: "true"
    policyResource: "true"
#selector:
#  matchLabels:
#    app: demo-app
spec:
  mtls:
    mode: DISABLE
```
```shell
oc apply -f resources/peer-authentication-mtls.yaml
```

2. Let's upgrade deployment of demo-app to listening on port 8083 instead of 8080, So it will be easier to sniff/capture traffic payloads: 
```shell
oc patch deployment demo-app-v1 --type='json' -p='[{"op": "replace", "path": "/spec/template/spec/containers/0/image", "value":"quay.io/zgrinber/demo-app:3"},{"op": "add", "path": "/spec/template/spec/containers/0/ports/1", "value": {"containerPort": 8083, "protocol": "TCP"}}]'
```

3. Deploy a client pod with envoy proxy sidecar
```shell
oc apply -f resources/rest-client-pod-sidecar.yaml -n demo
```

4. On a new Terminal window,Enter openshift worker node in debug mode, 
```shell
oc debug node/$(oc get node -l node-role.kubernetes.io/worker="" | grep -v NAME | awk '{print $1}')
```

5. Inside the node pod, sniff/intercept traffic on all network interfaces for port 8083:
```shell
tcpdump -i any port 8083 -XX
```

6. Go Back to the other terminal, and run the following command to invoke service from client pod (with side-car)
```shell
oc exec rest-api-client -n demo sh -- curl  -i http://demo-app-v1:8083/hello -H 'Password: SuperSecretPassword'
```

7. On the shell of the debugged node, you should see payload intercepted in plain-text ( because we disabled auto mTLS)
Output:
```shell
07:13:26.308972 IP ip-10-128-0-142.us-east-2.compute.internal.41530 > ip-10-128-0-121.us-east-2.compute.internal.us-srv: Flags [P.], seq 1:1545, ack 1, win 209, options [nop,nop,TS val 3560266732 ecr 3254560331], length 1544
        0x0000:  0004 0001 0006 0a58 0a80 008e 0000 0800  .......X........
        0x0010:  4500 063c 8dfc 4000 4006 90b9 0a80 008e  E..<..@.@.......
        0x0020:  0a80 0079 a23a 1f93 14ca bc99 a6a2 a8c7  ...y.:..........
        0x0030:  8018 00d1 1c35 0000 0101 080a d435 5bec  .....5.......5[.
        0x0040:  c1fc a64b 4745 5420 2f68 656c 6c6f 2048  ...KGET./hello.H
        0x0050:  5454 502f 312e 310d 0a68 6f73 743a 2064  TTP/1.1..host:.d
        0x0060:  656d 6f2d 6170 702d 7631 3a38 3038 330d  emo-app-v1:8083.
        0x0070:  0a75 7365 722d 6167 656e 743a 2063 7572  .user-agent:.cur
        0x0080:  6c2f 372e 3631 2e31 0d0a 6163 6365 7074  l/7.61.1..accept
        0x0090:  3a20 2a2f 2a0d 0a70 6173 7377 6f72 643a  :.*/*..password:
        0x00a0:  2053 7570 6572 5365 6372 6574 5061 7373  .SuperSecretPass
        0x00b0:  776f 7264 0d0a 782d 666f 7277 6172 6465  word..x-forwarde
        0x00c0:  642d 7072 6f74 6f3a 2068 7474 700d 0a78  d-proto:.http..x
        0x00d0:  2d72 6571 7565 7374 2d69 643a 2035 3431  -request-id:.541
        0x00e0:  6330 6464 312d 3538 3462 2d39 3665 332d  c0dd1-584b-96e3-
        0x00f0:  6232 6461 2d35 3435 3865 6163 3565 3638  b2da-5458eac5e68
        0x0100:  340d 0a78 2d65 6e76 6f79 2d64 6563 6f72  4..x-envoy-decor
        0x0110:  6174 6f72 2d6f 7065 7261 7469 6f6e 3a20  ator-operation:.
        0x0120:  6465 6d6f 2d61 7070 2d76 312e 6465 6d6f  demo-app-v1.demo
        0x0130:  2e73 7663 2e63 6c75 7374 6572 2e6c 6f63  .svc.cluster.loc
        0x0140:  616c 3a38 3038 332f 2a0d 0a78 2d65 6e76  al:8083/*..x-env
        0x0150:  6f79 2d70 6565 722d 6d65 7461 6461 7461  oy-peer-metadata
        0x0160:  3a20 4369 4d4b 446b 4651 5546 3944 5430  :.CiMKDkFQUF9DT0
        0x0170:  3555 5155 6c4f 5256 4a54 4568 4561 4433  5UQUlORVJTEhEaD3
        0x0180:  4a6c 6333 5174 5958 4270 4c57 4e73 6157  Jlc3QtYXBpLWNsaW
        0x0190:  5675 6441 6f61 4367 7044 5446 5654 5645  VudAoaCgpDTFVTVE
        0x01a0:  5653 5830 6c45 4567 7761 436b 7431 596d  VSX0lEEgwaCkt1Ym
        0x01b0:  5679 626d 5630 5a58 4d4b 4867 6f4d 5355  VybmV0ZXMKHgoMSU
        0x01c0:  3554 5645 464f 5130 5666 5356 4254 4567  5TVEFOQ0VfSVBTEg
        0x01d0:  3461 4444 4577 4c6a 4579 4f43 3477 4c6a  4aDDEwLjEyOC4wLj
        0x01e0:  4530 4d67 6f5a 4367 314a 5531 524a 5431  E0MgoZCg1JU1RJT1
        0x01f0:  3957 5256 4a54 5355 394f 4567 6761 426a  9WRVJTSU9OEggaBj
        0x0200:  4575 4d54 5175 4e51 712b 4177 6f47 5445  EuMTQuNQq+AwoGTE
        0x0210:  4643 5255 7854 4572 4d44 4b72 4144 436a  FCRUxTErMDKrADCj
        0x0220:  634b 4b47 5a68 6157 7831 636d 5574 5a47  cKKGZhaWx1cmUtZG
        0x0230:  3974 5957 6c75 4c6d 4a6c 6447 4575 6133  9tYWluLmJldGEua3
        0x0240:  5669 5a58 4a75 5a58 526c 6379 3570 6279  ViZXJuZXRlcy5pby
        0x0250:  3979 5a57 6470 6232 3453 4378 6f4a 6458  9yZWdpb24SCxoJdX
        0x0260:  4d74 5a57 467a 6443 3079 436a 594b 4a6d  MtZWFzdC0yCjYKJm
        0x0270:  5a68 6157 7831 636d 5574 5a47 3974 5957  ZhaWx1cmUtZG9tYW
        0x0280:  6c75 4c6d 4a6c 6447 4575 6133 5669 5a58  luLmJldGEua3ViZX
        0x0290:  4a75 5a58 526c 6379 3570 6279 3936 6232  JuZXRlcy5pby96b2
        0x02a0:  356c 4567 7761 436e 567a 4c57 5668 6333  5lEgwaCnVzLWVhc3
        0x02b0:  5174 4d6d 454b 4767 6f50 6257 4670 6333  QtMmEKGgoPbWFpc3
        0x02c0:  5279 5953 3132 5a58 4a7a 6157 3975 4567  RyYS12ZXJzaW9uEg
        0x02d0:  6361 4254 4975 4d79 3478 4368 674b 4133  caBTIuMy4xChgKA3
        0x02e0:  4a31 6268 4952 4767 3979 5a58 4e30 4c57  J1bhIRGg9yZXN0LW
        0x02f0:  4677 6153 316a 6247 6c6c 626e 514b 4a41  FwaS1jbGllbnQKJA
        0x0300:  6f5a 6332 566a 6458 4a70 6448 6b75 6158  oZc2VjdXJpdHkuaX
        0x0310:  4e30 6157 3875 6157 3876 6447 787a 5457  N0aW8uaW8vdGxzTW
        0x0320:  396b 5a52 4948 4767 5670 6333 5270 6277  9kZRIHGgVpc3Rpbw
        0x0330:  6f30 4368 397a 5a58 4a32 6157 4e6c 4c6d  o0Ch9zZXJ2aWNlLm
        0x0340:  6c7a 6447 6c76 4c6d 6c76 4c32 4e68 626d  lzdGlvLmlvL2Nhbm
        0x0350:  3975 6157 4e68 6243 3175 5957 316c 4568  9uaWNhbC1uYW1lEh
        0x0360:  4561 4433 4a6c 6333 5174 5958 4270 4c57  EaD3Jlc3QtYXBpLW
        0x0370:  4e73 6157 5675 6441 6f76 4369 4e7a 5a58  NsaWVudAovCiNzZX
        0x0380:  4a32 6157 4e6c 4c6d 6c7a 6447 6c76 4c6d  J2aWNlLmlzdGlvLm
        0x0390:  6c76 4c32 4e68 626d 3975 6157 4e68 6243  lvL2Nhbm9uaWNhbC
        0x03a0:  3179 5a58 5a70 6332 6c76 6268 4949 4767  1yZXZpc2lvbhIIGg
        0x03b0:  5a73 5958 526c 6333 514b 4877 6f5a 6447  ZsYXRlc3QKHwoZdG
        0x03c0:  3977 6232 7876 5a33 6b75 6158 4e30 6157  9wb2xvZ3kuaXN0aW
        0x03d0:  3875 6157 3876 6333 5669 656d 3975 5a52  8uaW8vc3Viem9uZR
        0x03e0:  4943 4767 414b 4c41 6f64 6447 3977 6232  ICGgAKLAoddG9wb2
        0x03f0:  7876 5a33 6b75 6133 5669 5a58 4a75 5a58  xvZ3kua3ViZXJuZX
        0x0400:  526c 6379 3570 6279 3979 5a57 6470 6232  Rlcy5pby9yZWdpb2
        0x0410:  3453 4378 6f4a 6458 4d74 5a57 467a 6443  4SCxoJdXMtZWFzdC
        0x0420:  3079 4369 734b 4733 5276 6347 3973 6232  0yCisKG3RvcG9sb2
        0x0430:  6435 4c6d 7431 596d 5679 626d 5630 5a58  d5Lmt1YmVybmV0ZX
        0x0440:  4d75 6157 3876 656d 3975 5a52 494d 4767  MuaW8vem9uZRIMGg
        0x0450:  7031 6379 316c 5958 4e30 4c54 4a68 4368  p1cy1lYXN0LTJhCh
        0x0460:  6f4b 4230 3146 5530 6866 5355 5153 4478  oKB01FU0hfSUQSDx
        0x0470:  6f4e 5932 7831 6333 526c 6369 3573 6232  oNY2x1c3Rlci5sb2
        0x0480:  4e68 6241 6f5a 4367 524f 5155 3146 4568  NhbAoZCgROQU1FEh
        0x0490:  4561 4433 4a6c 6333 5174 5958 4270 4c57  EaD3Jlc3QtYXBpLW
        0x04a0:  4e73 6157 5675 6441 6f54 4367 6c4f 5155  NsaWVudAoTCglOQU
        0x04b0:  3146 5531 4242 5130 5553 4268 6f45 5a47  1FU1BBQ0USBhoEZG
        0x04c0:  5674 6277 7045 4367 5650 5630 3546 5568  VtbwpECgVPV05FUh
        0x04d0:  4937 476a 6c72 6457 4a6c 636d 356c 6447  I7GjlrdWJlcm5ldG
        0x04e0:  567a 4f69 3876 5958 4270 6379 3932 4d53  VzOi8vYXBpcy92MS
        0x04f0:  3975 5957 316c 6333 4268 5932 567a 4c32  9uYW1lc3BhY2VzL2
        0x0500:  526c 6257 3876 6347 396b 6379 3979 5a58  RlbW8vcG9kcy9yZX
        0x0510:  4e30 4c57 4677 6153 316a 6247 6c6c 626e  N0LWFwaS1jbGllbn
        0x0520:  514b 4677 6f52 5545 7842 5645 5a50 556b  QKFwoRUExBVEZPUk
        0x0530:  3166 5455 5655 5155 5242 5645 4553 4169  1fTUVUQURBVEESAi
        0x0540:  6f41 4369 494b 4456 6450 556b 744d 5430  oACiIKDVdPUktMT0
        0x0550:  4645 5830 3542 5455 5553 4552 6f50 636d  FEX05BTUUSERoPcm
        0x0560:  567a 6443 3168 6347 6b74 5932 7870 5a57  VzdC1hcGktY2xpZW
        0x0570:  3530 0d0a 782d 656e 766f 792d 7065 6572  50..x-envoy-peer
        0x0580:  2d6d 6574 6164 6174 612d 6964 3a20 7369  -metadata-id:.si
        0x0590:  6465 6361 727e 3130 2e31 3238 2e30 2e31  decar~10.128.0.1
        0x05a0:  3432 7e72 6573 742d 6170 692d 636c 6965  42~rest-api-clie
        0x05b0:  6e74 2e64 656d 6f7e 6465 6d6f 2e73 7663  nt.demo~demo.svc
        0x05c0:  2e63 6c75 7374 6572 2e6c 6f63 616c 0d0a  .cluster.local..
        0x05d0:  782d 656e 766f 792d 6174 7465 6d70 742d  x-envoy-attempt-
        0x05e0:  636f 756e 743a 2031 0d0a 782d 6233 2d74  count:.1..x-b3-t
        0x05f0:  7261 6365 6964 3a20 6634 6638 6437 3764  raceid:.f4f8d77d
        0x0600:  3361 3739 3132 6535 3939 3665 3065 6337  3a7912e5996e0ec7
        0x0610:  6230 6635 6532 3663 0d0a 782d 6233 2d73  b0f5e26c..x-b3-s
        0x0620:  7061 6e69 643a 2039 3936 6530 6563 3762  panid:.996e0ec7b
        0x0630:  3066 3565 3236 630d 0a78 2d62 332d 7361  0f5e26c..x-b3-sa
        0x0640:  6d70 6c65 643a 2031 0d0a 0d0a            mpled:.1....
07:13:26.317286 IP ip-10-128-0-121.us-east-2.compute.internal.us-srv > ip-10-128-0-142.us-east-2.compute.internal.41530: Flags [P.], seq 1:1516, ack 1545, win 348, options [nop,nop,TS val 3254560339 ecr 3560266732], length 1515
        0x0000:  0003 0001 0006 0a58 0a80 0079 0000 0800  .......X...y....
        0x0010:  4500 061f d236 4000 4006 4c9c 0a80 0079  E....6@.@.L....y
        0x0020:  0a80 008e 1f93 a23a a6a2 a8c7 14ca c2a1  .......:........
        0x0030:  8018 015c 1c18 0000 0101 080a c1fc a653  ...\...........S
        0x0040:  d435 5bec 4854 5450 2f31 2e31 2032 3030  .5[.HTTP/1.1.200
        0x0050:  204f 4b0d 0a63 6f6e 7465 6e74 2d74 7970  .OK..content-typ
        0x0060:  653a 2061 7070 6c69 6361 7469 6f6e 2f6a  e:.application/j
        0x0070:  736f 6e0d 0a63 6f6e 7465 6e74 2d6c 656e  son..content-len
        0x0080:  6774 683a 2031 3137 0d0a 782d 656e 766f  gth:.117..x-envo
        0x0090:  792d 7570 7374 7265 616d 2d73 6572 7669  y-upstream-servi
        0x00a0:  6365 2d74 696d 653a 2033 0d0a 782d 656e  ce-time:.3..x-en
        0x00b0:  766f 792d 7065 6572 2d6d 6574 6164 6174  voy-peer-metadat
        0x00c0:  613a 2043 6838 4b44 6b46 5155 4639 4454  a:.Ch8KDkFQUF9DT
        0x00d0:  3035 5551 556c 4f52 564a 5445 6730 6143  05UQUlORVJTEg0aC
        0x00e0:  3252 6c62 5738 7459 5842 774c 5859 7843  2RlbW8tYXBwLXYxC
        0x00f0:  686f 4b43 6b4e 4d56 564e 5552 564a 6653  hoKCkNMVVNURVJfS
        0x0100:  5551 5344 426f 4b53 3356 695a 584a 755a  UQSDBoKS3ViZXJuZ
        0x0110:  5852 6c63 776f 6543 6778 4a54 6c4e 5551  XRlcwoeCgxJTlNUQ
        0x0120:  5535 4452 5639 4a55 464d 5344 686f 4d4d  U5DRV9JUFMSDhoMM
        0x0130:  5441 754d 5449 344c 6a41 754d 5449 7843  TAuMTI4LjAuMTIxC
        0x0140:  686b 4b44 556c 5456 456c 5058 315a 4655  hkKDUlTVElPX1ZFU
        0x0150:  6c4e 4a54 3034 5343 426f 474d 5334 784e  lNJT04SCBoGMS4xN
        0x0160:  4334 3143 7541 4443 675a 4d51 554a 4654  C41CuADCgZMQUJFT
        0x0170:  464d 5331 514d 7130 674d 4b45 516f 4459  FMS1QMq0gMKEQoDY
        0x0180:  5842 7745 676f 6143 4752 6c62 5738 7459  XBwEgoaCGRlbW8tY
        0x0190:  5842 7743 6a63 4b4b 475a 6861 5778 3163  XBwCjcKKGZhaWx1c
        0x01a0:  6d55 745a 4739 7459 576c 754c 6d4a 6c64  mUtZG9tYWluLmJld
        0x01b0:  4745 7561 3356 695a 584a 755a 5852 6c63  GEua3ViZXJuZXRlc
        0x01c0:  7935 7062 7939 795a 5764 7062 3234 5343  y5pby9yZWdpb24SC
        0x01d0:  786f 4a64 584d 745a 5746 7a64 4330 7943  xoJdXMtZWFzdC0yC
        0x01e0:  6a59 4b4a 6d5a 6861 5778 3163 6d55 745a  jYKJmZhaWx1cmUtZ
        0x01f0:  4739 7459 576c 754c 6d4a 6c64 4745 7561  G9tYWluLmJldGEua
        0x0200:  3356 695a 584a 755a 5852 6c63 7935 7062  3ViZXJuZXRlcy5pb
        0x0210:  7939 3662 3235 6c45 6777 6143 6e56 7a4c  y96b25lEgwaCnVzL
        0x0220:  5756 6863 3351 744d 6d45 4b47 676f 5062  WVhc3QtMmEKGgoPb
        0x0230:  5746 7063 3352 7959 5331 325a 584a 7a61  WFpc3RyYS12ZXJza
        0x0240:  5739 7545 6763 6142 5449 754d 7934 7843  W9uEgcaBTIuMy4xC
        0x0250:  6945 4b45 5842 765a 4331 305a 5731 7762  iEKEXBvZC10ZW1wb
        0x0260:  4746 305a 5331 6f59 584e 6f45 6777 6143  GF0ZS1oYXNoEgwaC
        0x0270:  6a56 6d4f 574d 304e 475a 6959 6a67 4b4a  jVmOWM0NGZiYjgKJ
        0x0280:  416f 5a63 3256 6a64 584a 7064 486b 7561  AoZc2VjdXJpdHkua
        0x0290:  584e 3061 5738 7561 5738 7664 4778 7a54  XN0aW8uaW8vdGxzT
        0x02a0:  5739 6b5a 5249 4847 6756 7063 3352 7062  W9kZRIHGgVpc3Rpb
        0x02b0:  776f 7443 6839 7a5a 584a 3261 574e 6c4c  wotCh9zZXJ2aWNlL
        0x02c0:  6d6c 7a64 476c 764c 6d6c 764c 324e 6862  mlzdGlvLmlvL2Nhb
        0x02d0:  6d39 7561 574e 6862 4331 7559 5731 6c45  m9uaWNhbC1uYW1lE
        0x02e0:  676f 6143 4752 6c62 5738 7459 5842 7743  goaCGRlbW8tYXBwC
        0x02f0:  6973 4b49 334e 6c63 6e5a 7059 3255 7561  isKI3NlcnZpY2Uua
        0x0300:  584e 3061 5738 7561 5738 7659 3246 7562  XN0aW8uaW8vY2Fub
        0x0310:  3235 7059 3246 734c 584a 6c64 6d6c 7a61  25pY2FsLXJldmlza
        0x0320:  5739 7545 6751 6141 6e59 7843 6838 4b47  W9uEgQaAnYxCh8KG
        0x0330:  5852 7663 4739 7362 3264 354c 6d6c 7a64  XRvcG9sb2d5Lmlzd
        0x0340:  476c 764c 6d6c 764c 334e 3159 6e70 7662  GlvLmlvL3N1Ynpvb
        0x0350:  6d55 5341 686f 4143 6977 4b48 5852 7663  mUSAhoACiwKHXRvc
        0x0360:  4739 7362 3264 354c 6d74 3159 6d56 7962  G9sb2d5Lmt1YmVyb
        0x0370:  6d56 305a 584d 7561 5738 7663 6d56 6e61  mV0ZXMuaW8vcmVna
        0x0380:  5739 7545 6773 6143 5856 7a4c 5756 6863  W9uEgsaCXVzLWVhc
        0x0390:  3351 744d 676f 7243 6874 3062 3342 7662  3QtMgorCht0b3Bvb
        0x03a0:  4739 6e65 5335 7264 574a 6c63 6d35 6c64  G9neS5rdWJlcm5ld
        0x03b0:  4756 7a4c 6d6c 764c 3370 7662 6d55 5344  GVzLmlvL3pvbmUSD
        0x03c0:  426f 4b64 584d 745a 5746 7a64 4330 7959  BoKdXMtZWFzdC0yY
        0x03d0:  516f 5043 6764 325a 584a 7a61 5739 7545  QoPCgd2ZXJzaW9uE
        0x03e0:  6751 6141 6e59 7843 686f 4b42 3031 4655  gQaAnYxChoKB01FU
        0x03f0:  3068 6653 5551 5344 786f 4e59 3278 3163  0hfSUQSDxoNY2x1c
        0x0400:  3352 6c63 6935 7362 324e 6862 416f 6d43  3Rlci5sb2NhbAomC
        0x0410:  6752 4f51 5531 4645 6834 6148 4752 6c62  gROQU1FEh4aHGRlb
        0x0420:  5738 7459 5842 774c 5859 784c 5456 6d4f  W8tYXBwLXYxLTVmO
        0x0430:  574d 304e 475a 6959 6a67 744e 6e67 3062  WM0NGZiYjgtNng0b
        0x0440:  586f 4b45 776f 4a54 6b46 4e52 564e 5151  XoKEwoJTkFNRVNQQ
        0x0450:  554e 4645 6759 6142 4752 6c62 5738 4b54  UNFEgYaBGRlbW8KT
        0x0460:  416f 4654 3164 4f52 5649 5351 7870 4261  AoFT1dORVISQxpBa
        0x0470:  3356 695a 584a 755a 5852 6c63 7a6f 764c  3ViZXJuZXRlczovL
        0x0480:  3246 7761 584d 7659 5842 7763 7939 324d  2FwaXMvYXBwcy92M
        0x0490:  5339 7559 5731 6c63 3342 6859 3256 7a4c  S9uYW1lc3BhY2VzL
        0x04a0:  3252 6c62 5738 765a 4756 7762 4739 3562  2RlbW8vZGVwbG95b
        0x04b0:  5756 7564 484d 765a 4756 7462 7931 6863  WVudHMvZGVtby1hc
        0x04c0:  4841 7464 6a45 4b46 776f 5255 4578 4256  HAtdjEKFwoRUExBV
        0x04d0:  455a 5055 6b31 6654 5556 5551 5552 4256  EZPUk1fTUVUQURBV
        0x04e0:  4545 5341 696f 4143 6834 4b44 5664 5055  EESAioACh4KDVdPU
        0x04f0:  6b74 4d54 3046 4558 3035 4254 5555 5344  ktMT0FEX05BTUUSD
        0x0500:  526f 4c5a 4756 7462 7931 6863 4841 7464  RoLZGVtby1hcHAtd
        0x0510:  6a45 3d0d 0a78 2d65 6e76 6f79 2d70 6565  jE=..x-envoy-pee
        0x0520:  722d 6d65 7461 6461 7461 2d69 643a 2073  r-metadata-id:.s
        0x0530:  6964 6563 6172 7e31 302e 3132 382e 302e  idecar~10.128.0.
        0x0540:  3132 317e 6465 6d6f 2d61 7070 2d76 312d  121~demo-app-v1-
        0x0550:  3566 3963 3434 6662 6238 2d36 7834 6d7a  5f9c44fbb8-6x4mz
        0x0560:  2e64 656d 6f7e 6465 6d6f 2e73 7663 2e63  .demo~demo.svc.c
        0x0570:  6c75 7374 6572 2e6c 6f63 616c 0d0a 6461  luster.local..da
        0x0580:  7465 3a20 5765 642c 2031 3520 4d61 7220  te:.Wed,.15.Mar.
        0x0590:  3230 3233 2030 373a 3133 3a32 3520 474d  2023.07:13:25.GM
        0x05a0:  540d 0a73 6572 7665 723a 2069 7374 696f  T..server:.istio
        0x05b0:  2d65 6e76 6f79 0d0a 0d0a 7b22 6672 6f6d  -envoy....{"from
        0x05c0:  223a 2254 6865 2047 7265 6574 696e 6720  ":"The.Greeting.
        0x05d0:  4170 706c 6963 6174 696f 6e20 5665 7273  Application.Vers
        0x05e0:  696f 6e3d 2076 3122 2c22 746f 223a 224a  ion=.v1","to":"J
        0x05f0:  6f68 6e20 446f 6522 2c22 7479 7065 223a  ohn.Doe","type":
        0x0600:  2247 656e 6572 616c 222c 2267 7265 6574  "General","greet
        0x0610:  696e 6722 3a22 5769 7368 2079 6f75 2048  ing":"Wish.you.H
        0x0620:  6170 7079 2052 6573 7469 6e67 2122 7d    appy.Resting!"}
```
**As you can see, sensitive data can be intercepted if not using mTLS between workloads.**

8. Let's enable mTLS on namespace back again by changing the tls mode of workloads in namespace demo to STRICT
```shell
oc patch peerauthentications.security.istio.io strict-policy --type='merge' -p '{"spec":{"mtls":{"mode": "STRICT"}}}'
```

9. Invoke Again request to demo-app service from rest-client pod
```shell
oc exec rest-api-client -n demo sh -- curl  -i http://demo-app-v1:8083/hello -H 'Password: SuperSecretPassword'
```
10. In the debugged node window, you should see now that all payloads now encrypted.
Output example:
```shell
07:24:46.857344 IP ip-10-128-0-142.us-east-2.compute.internal.57046 > ip-10-128-0-121.us-east-2.compute.internal.us-srv: Flags [P.], seq 381:2389, ack 2284, win 348, options [nop,nop,TS val 3560947280 ecr 3255240877], length 2008
        0x0000:  0004 0001 0006 0a58 0a80 008e 0000 0800  .......X........
        0x0010:  4500 080c 6884 4000 4006 b461 0a80 008e  E...h.@.@..a....
        0x0020:  0a80 0079 ded6 1f93 febb f9a3 38eb 4a98  ...y........8.J.
        0x0030:  8018 015c 1e05 0000 0101 080a d43f be50  ...\.........?.P
        0x0040:  c207 08ad 1403 0300 0101 1703 0306 65dc  ..............e.
        0x0050:  2d7b 9df7 2e35 cde0 3b31 6f8f 997d b4d8  -{...5..;1o..}..
        0x0060:  5f3d 80e7 53c0 2806 f046 b3c3 23c9 e7d4  _=..S.(..F..#...
        0x0070:  40c2 8932 4118 1a34 e1bc c222 569b 02fa  @..2A..4..."V...
        0x0080:  f5ad c0dd 3971 c923 6e78 578b c89d 6a6a  ....9q.#nxW...jj
        0x0090:  2e3a 6e72 d200 d101 a8eb 4f36 c987 b1e9  .:nr......O6....
        0x00a0:  23cd e8c9 0405 dde8 f057 b84d 5b5c 3646  #........W.M[\6F
        0x00b0:  a53a ab12 b03e 7cd7 5824 0b8c beaf 12bb  .:...>|.X$......
        0x00c0:  4ef5 6982 979a 434b 571b b80d b4ba e5aa  N.i...CKW.......
        0x00d0:  99bb 9375 d423 60f0 74bc d448 7302 4e62  ...u.#`.t..Hs.Nb
        0x00e0:  2657 3213 29fb ff1c 0ebe b053 593c 4e0a  &W2.)......SY<N.
        0x00f0:  8548 8138 8605 1500 07a4 7f82 152c 766c  .H.8.........,vl
        0x0100:  00a2 2e5e d7b8 b9d1 5452 87f0 c012 9e53  ...^....TR.....S
        0x0110:  e771 0818 0e1b 06ea c80b a30b ac9e 3572  .q............5r
        0x0120:  514e 51b6 2b66 f24c f4be 94cb 2039 8950  QNQ.+f.L.....9.P
        0x0130:  13c6 793e 416f 9af0 5c5b bf57 2825 feef  ..y>Ao..\[.W(%..
        0x0140:  79c7 6398 8152 948b ea86 e680 4151 0b1a  y.c..R......AQ..
        0x0150:  5dcc 1c26 316f 32e1 7545 805f ff1c 1879  ]..&1o2.uE._...y
        0x0160:  c3cd b76e ab76 c384 dff0 67ea 7c8d 032e  ...n.v....g.|...
        0x0170:  00e2 c5f9 7114 6809 5d06 8b89 bc1f 4dbf  ....q.h.].....M.
        0x0180:  6030 6a46 995b 50d0 ba15 9888 385c d089  `0jF.[P.....8\..
        0x0190:  a789 300b e2d0 b16f bf70 aa2f f3ff 1606  ..0....o.p./....
        0x01a0:  661a 2815 2015 ac23 5ee8 918d 7084 38e0  f.(....#^...p.8.
        0x01b0:  111b a058 6873 782a 01b5 d398 2bd0 81d4  ...Xhsx*....+...
        0x01c0:  61b0 bd11 e262 e460 8deb 22ac 5905 18fa  a....b.`..".Y...
        0x01d0:  a8d5 4cbe b4bb 6337 f034 3818 a97e c7dc  ..L...c7.48..~..
        0x01e0:  5f9a ddd8 9b07 fa55 b5c4 447e 0fd5 845a  _......U..D~...Z
        0x01f0:  594e ec62 4520 5cb1 01e2 9980 59fd 7a63  YN.bE.\.....Y.zc
        0x0200:  fe09 ce1a 9e24 599c fe2a 56ab 8c9d 3033  .....$Y..*V...03
        0x0210:  2e76 81ae 6e73 ffc8 55b3 b467 3e24 dc70  .v..ns..U..g>$.p
        0x0220:  2f86 e6cf 1f3d 7bc1 e102 990c 7d50 141d  /....={.....}P..
        0x0230:  e6c6 838a 3aff 4c55 04fc 9265 ed4f ea86  ....:.LU...e.O..
        0x0240:  e023 6af4 7c5a fce0 2dbe 52dc 4968 4744  .#j.|Z..-.R.IhGD
        0x0250:  4f5d f64e b619 2c1b 6a5b a817 e21b 500c  O].N..,.j[....P.
        0x0260:  4cd5 98c9 a343 afbf dca7 9c9d 3ae0 4f55  L....C......:.OU
        0x0270:  305a 488a cb62 0ae8 389d 9630 1a41 958b  0ZH..b..8..0.A..
        0x0280:  5012 1167 6f22 fe13 2bb9 7b38 b901 b3c8  P..go"..+.{8....
        0x0290:  a279 9ecc e24f 5bd0 cfc7 72a2 362b 1ac8  .y...O[...r.6+..
        0x02a0:  d919 a204 6346 ebe7 56fa 93d8 eff6 d0b0  ....cF..V.......
        0x02b0:  b811 1a69 a64f 0490 dfb8 1885 bceb 352a  ...i.O........5*
        0x02c0:  d6ac 5743 729d e51d 8d2c 95dc 2090 6a2f  ..WCr....,....j/
        0x02d0:  8500 a4db d252 bc7b 74a1 1888 c3bd 1ddb  .....R.{t.......
        0x02e0:  9e63 5136 2b86 4e37 d9ad 8d21 d70d e447  .cQ6+.N7...!...G
        0x02f0:  4ba4 0dae f339 5df8 670f cbac c2ff 3a8c  K....9].g.....:.
        0x0300:  c3c3 42a0 9f7e 94be 3fbb b674 abee fb3e  ..B..~..?..t...>
        0x0310:  b7f9 234c c37f 5f64 3be6 bd9b ba8c ef3e  ..#L.._d;......>
        0x0320:  084e 2582 6a87 9aaf ce3a 967c 115f ab16  .N%.j....:.|._..
        0x0330:  f69f b32c e8a0 151d 2d41 c41b 27bc ae55  ...,....-A..'..U
        0x0340:  6a1e a427 0a16 746d 9c55 8910 ce65 4376  j..'..tm.U...eCv
        0x0350:  c2dc 4e3f 5eae 91ae c25b 21ae 531d 4321  ..N?^....[!.S.C!
        0x0360:  a306 fc08 a66a 3be3 a906 423c 58ba 9d26  .....j;...B<X..&
        0x0370:  7b9d 8e37 4d0e 4713 c00e 7961 b350 67e0  {..7M.G...ya.Pg.
        0x0380:  6aff 95ae bcea e475 018e 8c5a f7ff 9455  j......u...Z...U
        0x0390:  fedb 1c5c 1df4 4792 2c47 7e4c 753c 29f4  ...\..G.,G~Lu<).
        0x03a0:  fc39 546e 387f ffa6 cd0e 1a20 5548 8b63  .9Tn8.......UH.c
        0x03b0:  45af 1f94 3a1a 6020 ea20 becc 0fb9 6cbd  E...:.`.......l.
        0x03c0:  93b8 3b73 50c2 0ae9 8bad d871 ead1 b57a  ..;sP......q...z
        0x03d0:  a331 0d70 012b 7b07 872d 41e7 9527 9888  .1.p.+{..-A..'..
        0x03e0:  42a7 7290 c0ec fafa 3b35 12e0 6dc4 fa0a  B.r.....;5..m...
        0x03f0:  b4b9 cc49 d672 6d0e 5482 954b 72df 6235  ...I.rm.T..Kr.b5
        0x0400:  dd9b aab8 58df da87 e2fb b415 d5ab 7f7f  ....X...........
        0x0410:  f2c8 dc28 df0c 0a3c f808 2d74 07ef a65e  ...(...<..-t...^
        0x0420:  a10d 450d c1f7 0fda 40ea ddc7 fbd5 5997  ..E.....@.....Y.
        0x0430:  ba00 2782 f423 6dd8 7aff a7de fb66 2672  ..'..#m.z....f&r
        0x0440:  5487 2efa ed8d 2569 9be3 ebc3 5216 cd86  T.....%i....R...
        0x0450:  ee15 0ecf c209 9bbd 6bb3 106b b3dd be0b  ........k..k....
        0x0460:  6612 2396 dc60 6c46 1cea 1ee9 4127 44b6  f.#..`lF....A'D.
        0x0470:  1253 0ddd 28ce 3d97 a779 01ee b530 bb17  .S..(.=..y...0..
        0x0480:  94aa a6f9 4468 fc6d f4ae e39e 78dc 74f1  ....Dh.m....x.t.
        0x0490:  d610 52bb f507 ab0f 3b73 e510 f1fe f569  ..R.....;s.....i
        0x04a0:  dd4b 31ff 47f2 300d e56f 8876 a9a2 c164  .K1.G.0..o.v...d
        0x04b0:  6f6f 20f1 d088 5b60 61c4 a75f 4308 30b5  oo....[`a.._C.0.
        0x04c0:  8f45 bd68 2e02 3c1d ffa3 dddd d2f7 eecc  .E.h..<.........
        0x04d0:  563a 21b4 d65a c5b4 d6f5 379b e1c5 6448  V:!..Z....7...dH
        0x04e0:  8cfa 98d1 cd4c fd85 a534 8023 4f5d e7f3  .....L...4.#O]..
        0x04f0:  2d2e a9e2 7431 d200 d680 785e 9b88 4770  -...t1....x^..Gp
        0x0500:  35f5 390b 94a6 f8dd 02c3 67b2 5987 bdb5  5.9.......g.Y...
        0x0510:  ad93 de96 822b 4237 3aec c19b ee66 6edb  .....+B7:....fn.
        0x0520:  e22d d2ee 70bc 1c82 36d5 4d57 d26a c1f6  .-..p...6.MW.j..
        0x0530:  4600 83bd 18b6 6bcb 9c8d 65ae 24d7 d220  F.....k...e.$...
        0x0540:  5710 afa9 e740 6cfd 1a13 e61d dc8d a27e  W....@l........~
        0x0550:  711a 7ef9 7a2a f1d6 2aa2 1f21 c3d2 2521  q.~.z*..*..!..%!
        0x0560:  c0e5 3ec9 8269 f04c 514e 9b39 788a b7de  ..>..i.LQN.9x...
        0x0570:  2811 f87e 39fc db65 d793 7277 9cd4 75ce  (..~9..e..rw..u.
        0x0580:  6359 4fea 0aac 1b74 12be 1831 08bb 1cd5  cYO....t...1....
        0x0590:  5c53 ad2d e534 3f3b bc37 04fb c991 ad43  \S.-.4?;.7.....C
        0x05a0:  7c59 35e2 42d6 89d6 a608 511e 9513 e318  |Y5.B.....Q.....
        0x05b0:  3a20 0a79 cbc0 dd0f 5cd0 5cb8 bb54 ac6b  :..y....\.\..T.k
        0x05c0:  611d 188c 5b5b 98a3 3fab 5d34 8bd8 3a8d  a...[[..?.]4..:.
        0x05d0:  05f8 2940 c1ac f098 acf1 f7d7 9846 e42c  ..)@.........F.,
        0x05e0:  3e38 711a e0d4 bba6 e99f 00d5 cb7c 407c  >8q..........|@|
        0x05f0:  095c 01f0 488c 28cf cd28 105f c8a7 a787  .\..H.(..(._....
        0x0600:  aaa2 a8a5 a741 6ea2 5432 64cd 957c 8f1f  .....An.T2d..|..
        0x0610:  2524 8a0d 2b57 3f94 74d0 9f44 c13d 3da6  %$..+W?.t..D.==.
        0x0620:  52f0 7e9f cec2 97fc 12bf eeeb 3d99 b1f7  R.~.........=...
        0x0630:  6d04 5c80 6197 867e e089 5425 a857 0dbe  m.\.a..~..T%.W..
        0x0640:  a1ec 1661 9737 cd8d 0d99 31bd 0a3f a513  ...a.7....1..?..
        0x0650:  5156 c62a 7e69 0246 5366 932c 0896 049c  QV.*~i.FSf.,....
        0x0660:  b378 b6e6 9903 f978 3405 440d 5a78 e316  .x.....x4.D.Zx..
        0x0670:  1a4c 2148 74d2 9460 4f73 b33c f184 9703  .L!Ht..`Os.<....
        0x0680:  2e62 2b6c 4987 97e2 9436 16b9 5df2 d509  .b+lI....6..]...
        0x0690:  47f6 733d 3618 3607 67ef adb0 f0aa 9322  G.s=6.6.g......"
        0x06a0:  1eb3 8735 b4f7 9d2f 79e5 fd36 fa28 23ab  ...5.../y..6.(#.
        0x06b0:  070d 6929 1703 0301 19f0 6bb3 2d08 3647  ..i)......k.-.6G
        0x06c0:  83fc 63a4 7d82 84d0 4fc1 d40b 92c8 2bbc  ..c.}...O.....+.
        0x06d0:  6a35 e01e 14fb eddf 8ba0 5fb9 221e ada7  j5........_."...
        0x06e0:  cd34 0cfc 2878 2c23 b900 b7b6 94df c460  .4..(x,#.......`
        0x06f0:  6c8e 944e bd14 1230 7c6b 7cc9 ee87 69bc  l..N...0|k|...i.
        0x0700:  3bcd 5660 9d1d a2eb 62fe a97d 2990 b954  ;.V`....b..})..T
        0x0710:  7c93 a9e4 77ab 9dab e21f 3668 95b3 b3c7  |...w.....6h....
        0x0720:  974a 0b07 869e ff07 be34 6404 2276 8e03  .J.......4d."v..
        0x0730:  8cd3 c208 9847 2803 b434 6335 d988 9d5d  .....G(..4c5...]
        0x0740:  8185 930c bac8 e49e 878f 5d9a db5f 7266  ..........].._rf
        0x0750:  ecf9 674d b84e 62ff 232c 27c8 8f0a 51ae  ..gM.Nb.#,'...Q.
        0x0760:  91c4 082b 029e a866 54d0 5852 ecdb 6029  ...+...fT.XR..`)
        0x0770:  5c32 bceb 9c98 300a e921 f7d1 7c1c 8d71  \2....0..!..|..q
        0x0780:  adc0 9083 8862 27c0 31f4 5e60 9b44 a4f2  .....b'.1.^`.D..
        0x0790:  3b45 2088 9997 e811 7bfb 3157 27f1 bcd8  ;E......{.1W'...
        0x07a0:  0dc2 3cb5 7a51 12db c346 885d 5a22 0377  ..<.zQ...F.]Z".w
        0x07b0:  ace4 8ea1 b575 deec d674 41b4 697a 0d19  .....u...tA.iz..
        0x07c0:  ece8 6d9a 37b4 e58d e7bd 189f bbdd bc62  ..m.7..........b
        0x07d0:  7e63 1703 0300 4577 35c4 7695 2cc9 d7be  ~c....Ew5.v.,...
        0x07e0:  5ec0 4968 b466 b4d3 9fae 04b2 0ea7 8813  ^.Ih.f..........
        0x07f0:  029d 1a4b 39c8 f010 6f79 f967 5a7e f330  ...K9...oy.gZ~.0
        0x0800:  f7bf 1023 3999 66d3 c376 2cea c25a 1da3  ...#9.f..v,..Z..
        0x0810:  36e1 a76a 9245 e082 7f62 2a50            6..j.E...b*P
07:24:46.857445 IP ip-10-128-0-142.us-east-2.compute.internal.57046 > ip-10-128-0-121.us-east-2.compute.internal.us-srv: Flags [P.], seq 2389:3955, ack 2284, win 348, options [nop,nop,TS val 3560947280 ecr 3255240877], length 1566
        0x0000:  0003 0001 0006 0a58 0a80 008e 0000 0800  .......X........
        0x0010:  4500 0652 6885 4000 4006 b61a 0a80 008e  E..Rh.@.@.......
        0x0020:  0a80 0079 ded6 1f93 febc 017b 38eb 4a98  ...y.......{8.J.
        0x0030:  8018 015c 1c4b 0000 0101 080a d43f be50  ...\.K.......?.P
        0x0040:  c207 08ad 1703 0306 19d2 c1df 1123 a110  .............#..
        0x0050:  1b5e 45c8 5b65 8beb 2996 41e7 7322 0f94  .^E.[e..).A.s"..
        0x0060:  5e7e 345c 5e19 5d6d 824e b943 20fd 92df  ^~4\^.]m.N.C....
        0x0070:  8e31 b809 f026 11c0 32ea a4fb f955 cdca  .1...&..2....U..
        0x0080:  5fda fd6a 94ea e422 16fa 4185 1694 2eb1  _..j..."..A.....
        0x0090:  2010 1ad0 aa9d f3ff c735 ecd2 51bc 026b  .........5..Q..k
        0x00a0:  b7e1 12ff 4b0c c5db 8b4a d088 3ef4 0c1d  ....K....J..>...
        0x00b0:  b7c7 25b9 cc56 405a 72b1 6c54 b872 944b  ..%..V@Zr.lT.r.K
        0x00c0:  f46a a052 8c7c cd04 9b8b 9943 9daa 1b2e  .j.R.|.....C....
        0x00d0:  f6f3 a184 c79c 7411 7018 68fe 6759 0524  ......t.p.h.gY.$
        0x00e0:  3aac 0574 f2d0 01d4 1ec3 f228 7d80 83bc  :..t.......(}...
        0x00f0:  e41b 2e58 4c95 a3e0 5cde 9ce8 e7ca 8b2a  ...XL...\......*
        0x0100:  b2f1 2d52 2e1c 8719 60ea 7be1 715a a606  ..-R....`.{.qZ..
        0x0110:  7cb4 1839 f815 a5bc 1683 e428 80d1 9cf0  |..9.......(....
        0x0120:  5814 6c77 0398 d427 21c0 0635 ab56 6a27  X.lw...'!..5.Vj'
        0x0130:  31aa 2734 e0fd 68e2 14b9 5b24 6ac0 3dd5  1.'4..h...[$j.=.
        0x0140:  62b2 8d82 d762 1e08 6e66 c237 835c ee41  b....b..nf.7.\.A
        0x0150:  b267 a716 2858 cc7c 1608 1d7a ec27 1e3e  .g..(X.|...z.'.>
        0x0160:  60cb 2a3f 1730 e52a 6675 1cbe a880 b75c  `.*?.0.*fu.....\
        0x0170:  742b a598 83e5 a8b5 87da 7fb0 6d60 0b03  t+..........m`..
        0x0180:  742c 219f 94a2 3139 0ec8 5e0c 31cb 14a4  t,!...19..^.1...
        0x0190:  8b19 58d4 dc2f 8b46 3908 916d d814 e970  ..X../.F9..m...p
        0x01a0:  7c7e 2c05 ecf9 55fc a160 7471 845d 6755  |~,...U..`tq.]gU
        0x01b0:  d086 aa42 6677 5891 a18f b808 c887 9944  ...BfwX........D
        0x01c0:  2a45 a287 78ee c180 dedb 320b e44a cf56  *E..x.....2..J.V
        0x01d0:  28be 3e7b ce88 ef0e 341f 2d16 9f7b a98f  (.>{....4.-..{..
        0x01e0:  95f8 da40 d343 90af fb81 f5db 21fd 4a88  ...@.C......!.J.
        0x01f0:  a9d0 375f 7dc2 0448 0fb6 90ec b93c 5d91  ..7_}..H.....<].
        0x0200:  0923 0ac2 40d4 4380 4e76 fbe9 9496 be7f  .#..@.C.Nv......
        0x0210:  d9c0 6cc5 6ef1 753c 92c9 625d 19c1 cdaa  ..l.n.u<..b]....
        0x0220:  b3b5 6edf 7330 1c1a c46a 1df4 4e4c b49c  ..n.s0...j..NL..
        0x0230:  96cc 7107 7801 1655 9729 7f19 cb16 6fb5  ..q.x..U.)....o.
        0x0240:  d43c fa5f 57ed 0b2b 6592 f5e1 b0b8 ad81  .<._W..+e.......
        0x0250:  f99b 998a fe5e 2cb3 634a 6655 c8c7 06a3  .....^,.cJfU....
        0x0260:  5d1b d3ec 01b2 1516 4bb6 acad 32cd eeb7  ].......K...2...
        0x0270:  a21a 3442 7708 0623 ec0e 005f 5f15 4d09  ..4Bw..#...__.M.
        0x0280:  6a73 f414 b8a8 f7cc 53d1 c49e 7a27 6e92  js......S...z'n.
        0x0290:  04a0 7c2f be6d 81eb 1d53 ad23 ecdc 41eb  ..|/.m...S.#..A.
        0x02a0:  5c81 9553 999d 9769 e05a d5a2 27ac df2f  \..S...i.Z..'../
        0x02b0:  78da 98c6 86b6 13a6 ec78 62e8 6f2e 18f9  x........xb.o...
        0x02c0:  6a32 882c 4244 9240 a486 b294 1dc7 7d39  j2.,BD.@......}9
        0x02d0:  136b 8ea7 f159 92bd 79ca 192f a440 499a  .k...Y..y../.@I.
        0x02e0:  fd67 8f4b 41a4 4204 8d42 be9c 2bcd 5612  .g.KA.B..B..+.V.
        0x02f0:  6a95 00ed 44ef 21b1 8ec0 b679 71cb ac23  j...D.!....yq..#
        0x0300:  cfae 73b1 9e75 090a 3475 4d2c 9c89 c627  ..s..u..4uM,...'
        0x0310:  3e8c c633 8d60 2aff 9fba e7dd da98 2d01  >..3.`*.......-.
        0x0320:  6937 e2d6 f6aa b49f 72c4 0aba 1454 efc4  i7......r....T..
        0x0330:  bd8a 9f67 a0d2 a0a8 6f9e 517b 45ea 4603  ...g....o.Q{E.F.
        0x0340:  9b7f 3244 a927 b708 7e95 bc52 03bb 803c  ..2D.'..~..R...<
        0x0350:  2445 a69e 1038 16ef 7f25 f1e1 6aa4 e63a  $E...8...%..j..:
        0x0360:  c9d1 87b3 500d 9759 fdc0 8dcc cb6b 5863  ....P..Y.....kXc
        0x0370:  d292 a95e 32a7 5122 3103 e116 a594 e117  ...^2.Q"1.......
        0x0380:  eca9 3f04 ee08 4279 abdf 0a72 0228 02a0  ..?...By...r.(..
        0x0390:  9feb 5f81 87c6 366d e6c0 dd0c d583 0779  .._...6m.......y
        0x03a0:  0860 fdc9 ede8 554b 5d02 bb2c b617 9604  .`....UK]..,....
        0x03b0:  41e1 a73f d826 a2c3 89b4 cf36 99a1 08a2  A..?.&.....6....
        0x03c0:  6175 5665 6652 156e 8251 1ed6 df2a 3265  auVefR.n.Q...*2e
        0x03d0:  0345 f188 427e f7f4 031f 7489 31c1 8ad0  .E..B~....t.1...
        0x03e0:  d2e8 fc07 8632 1159 a7ba 846c 664c 1347  .....2.Y...lfL.G
        0x03f0:  6d1e 185b 085d 44a3 60f8 a57c bf1c f41f  m..[.]D.`..|....
        0x0400:  8a2f 79a5 b8a1 1c47 f154 b2de dead ab1a  ./y....G.T......
        0x0410:  6e9b 8bf4 f5c2 54ca cbd1 0777 df39 e39e  n.....T....w.9..
        0x0420:  a07b 668f 78d6 e0f4 5475 dd11 36ac e04a  .{f.x...Tu..6..J
        0x0430:  9f34 188a e349 3515 2a1b b47a a950 e104  .4...I5.*..z.P..
        0x0440:  6bb2 e6a9 b11b 924d 266b 77ea c90d 91a2  k......M&kw.....
        0x0450:  9d54 1e18 400c ba0e 9d96 0439 60e5 db8f  .T..@......9`...
        0x0460:  613a 3c4b fcb4 71ee 4fea 9b17 f725 580c  a:<K..q.O....%X.
        0x0470:  107d bd4a db92 9dea b9a0 69a1 7f8d fe27  .}.J......i....'
        0x0480:  0bef f50e b583 94d0 8f14 649e 288e d082  ..........d.(...
        0x0490:  119d 2141 20d9 3a72 0f99 6f09 d460 f83a  ..!A..:r..o..`.:
        0x04a0:  a2b5 33c1 f900 2447 0d41 4a8c 0545 e5ca  ..3...$G.AJ..E..
        0x04b0:  d8b7 ce67 e0d6 cb2b c9d3 2e5a bb61 f62e  ...g...+...Z.a..
        0x04c0:  5192 adcc 70f6 b9aa 2ee3 0327 c14c 0f86  Q...p......'.L..
        0x04d0:  b290 9870 6a92 0f8e 9891 77c3 cf90 fa72  ...pj.....w....r
        0x04e0:  e198 544a b02e 4d19 acd3 7f22 d10e d7ba  ..TJ..M...."....
        0x04f0:  05fe eb8a d964 12a1 64d8 625e 60c6 d1b0  .....d..d.b^`...
        0x0500:  f95a f432 4dda e298 d5b4 9bac d5c1 8ccf  .Z.2M...........
        0x0510:  8038 e9cb 54be 6698 583c 0390 84c7 f880  .8..T.f.X<......
        0x0520:  f090 94de 6e30 c610 c79d f434 bbb8 a540  ....n0.....4...@
        0x0530:  4148 3216 b97a ee30 ca5a 3984 56b7 4020  AH2..z.0.Z9.V.@.
        0x0540:  341b 8974 d2a2 6659 8eba b205 7d75 5eb2  4..t..fY....}u^.
        0x0550:  4ede a33b 5bb0 671a 1636 2b32 8699 7720  N..;[.g..6+2..w.
        0x0560:  cef6 51fd bf0e 8bc5 71a3 55d2 eed9 1602  ..Q.....q.U.....
        0x0570:  6a94 4529 8dfb 05b7 a7ca f0c7 19d3 3d7b  j.E)..........={
        0x0580:  1d3e 71f5 b72f d9b7 325b 8c5f f4a5 0175  .>q../..2[._...u
        0x0590:  e5c5 55e8 9e7e 1e2c 43a2 a95d bad3 b903  ..U..~.,C..]....
        0x05a0:  a208 eeef 5f4f e41d 2899 0eca 0686 96ca  ...._O..(.......
        0x05b0:  4a66 346a 09db aeb4 693c e3c9 24f6 9f86  Jf4j....i<..$...
        0x05c0:  a6ed 9cd6 00f4 e71b 3f71 8946 a270 4422  ........?q.F.pD"
        0x05d0:  cbd8 d97e dbf2 2763 325b b5a1 e193 ab69  ...~..'c2[.....i
        0x05e0:  a7ce 68e8 bedc 9e54 0d3e 27e3 39e3 d5cf  ..h....T.>'.9...
        0x05f0:  2435 82d0 3ac9 8698 ae36 5d08 4d2c 683d  $5..:....6].M,h=
        0x0600:  2417 9220 34b0 7ffd e8df 7f57 bf26 baab  $...4......W.&..
        0x0610:  a880 41ff ddb1 8c52 3ec4 bd31 a19f 4169  ..A....R>..1..Ai
        0x0620:  d1ba 81de 8621 dab6 afb8 8d5b 0ca5 a019  .....!.....[....
        0x0630:  fe14 6d41 19eb e61f d831 5367 9809 684c  ..mA.....1Sg..hL
        0x0640:  4bfd f0a8 0de0 1439 661e 281a 6d71 b07d  K......9f.(.mq.}
        0x0650:  6c43 4f69 4f0b 64a6 6b51 ae68 9226 262e  lCOiO.d.kQ.h.&&.
        0x0660:  b8f5                                     
```
Note: Basically the http payload "upgraded" to TLS encrypted, by mTLS enabled between 2 workloads in namespace( envoy proxy to envoy proxy)  so the communication channel between the two pods is encrypted
11. press CTRL +C to stop sniffing , and Exit debugged node' pod:
```shell
exit
```
Output:
```shell
20 packets captured
82 packets received by filter
54 packets dropped by kernel
sh-4.4# exit
exit

Removing debug pod ...
[zgrinber@zgrinber rh-service-mesh-demos]$ 
```
12. Now create a rest-client-pod without envoy proxy sidecar:
```shell
oc run -it rest-api-client-no-proxy  --image=ubi8/ubi:8.5-226 -n demo --command -- bash
exit
```
13. Try to invoke service demo-app from that client:
```shell
oc exec rest-api-client-no-proxy -n demo sh -- curl  -i http://demo-app-v1:8083/hello -H 'Password: SuperSecretPassword'
```
Output:
```shell
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0
curl: (56) Recv failure: Connection reset by peer
command terminated with exit code 56 
```
Remark: It was rejected because RH Service mesh istio imposes a policy of tls mode = STRICT, hence allowing only TLS payload , and not plaintext.
14. Let's get back to RH Service mesh istio default tls mode ( Permissive):
```shell
oc patch peerauthentications.security.istio.io strict-policy --type='merge' -p '{"spec":{"mtls":{"mode": "PERMISSIVE"}}}'
```
15. Wait few seconds and invoke again the Service using HTTP Call:
```shell
oc exec rest-api-client-no-proxy -n demo sh -- curl  -i http://demo-app-v1:8083/hello -H 'Password: SuperSecretPassword'
```
Output:
```shell
 % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   125  100   125    0     0  12500      0 --:--:-- --:--:-- --:--:-- 12500
HTTP/1.1 200 OK
content-type: application/json
content-length: 125
x-envoy-upstream-service-time: 4
date: Wed, 15 Mar 2023 07:36:52 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8083/*

{"from":"The Greeting Application Version= v1","to":"John Doe","type":"General","greeting":"Wish you prosperity and Wealth!"}
```
## Allow Access from same namespace
First, Redeploy again MS demo-app with port 8080
```shell
oc apply -f ../demo-application/openshift-manifests/v1/manifests.yaml
```
1. Apply the following `AuthorizationPolicy`
```shell
oc apply -f resources/authorization-policy-allow-same-namespace-custom.yaml
```

2. Try to access the demo-app service using a pod that is in the mesh:
```shell
oc exec rest-api-client -n demo  -- curl -i http://demo-app-v1:8080/hello -H 'Password: SuperSecretPassword'
```
Output:
```shell
   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0    451      0 --:--:-- --:--:-- --:--:--   449
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 188
date: Wed, 15 Mar 2023 07:48:18 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
```

3. Try now to access the Service from pod that is outside the mesh ( Without envoy proxy side-car)
```shell
oc exec rest-api-client-no-proxy -n demo -- curl  -i http://demo-app-v1:8080/hello
```
Output:
```shell
HTTP/1.1 403 Forbidden
content-length: 19
content-type: text/plain
date: Wed, 15 Mar 2023 07:50:53 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8080/*

RBAC: access denied
```
4. Deploy a client pod with envoy proxy sidecar in demo-test namespace:
```shell
oc apply -f resources/rest-client-pod-sidecar.yaml -n demo-test
```
5. Try to access from rest client pod that is in a different namespace demo-test, and you should be unauthorized because the policy above only allow traffic to demo-app workload from the same namespace
```shell
oc exec rest-api-client -n demo-test  -- curl -i http://demo-app-v1.demo:8080/hello
```
Output:
```shell
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    19  100    19    0     0   9500      0 --:--:-- --:--:-- --:--:--  9500
HTTP/1.1 403 Forbidden
content-length: 19
content-type: text/plain
date: Wed, 15 Mar 2023 07:57:14 GMT
server: envoy
x-envoy-upstream-service-time: 1

RBAC: access denied
```
## Allow Access from different namespace.
1. delete same namespace `AuthorizationPolicy`
```shell
oc delete -f resources/authorization-policy-allow-same-namespace-custom.yaml 
```
2. Apply new `AuthorizationPolicy` that will allow accessing to our workload from 2 namespaces, demo and demo-test:
```shell
oc apply -f resources/authorization-policy-allow-2-namespaces-custom.yaml
```

3. Wait 10-15 seconds, Now try again to invoke the service from demo-test namespace
```shell
 oc exec rest-api-client -n demo-test  -- curl -i http://demo-app-v1.demo:8080/hello
```
Output:
```shell
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 4
date: Wed, 15 Mar 2023 08:04:36 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
```
## Allow Access based on a given header' value.
1. Delete former policy:
```shell
oc delete -f resources/authorization-policy-allow-2-namespaces-custom.yaml
```

2. Apply new `AuthorizationPolicy` that will only allow traffic from same namespace if the request has header user with value prefixed with 'admin-' value: 
```shell
oc apply -f resources/authorization-policy-allow-based-on-header.yaml
```
3. Try to invoke twice the service from same namespace, one time without user header, and one time with the user header with unmatched value:
```shell
 oc exec rest-api-client -n demo  -- curl -i http://demo-app-v1:8080/hello -H 'user: zvi'
 oc exec rest-api-client -n demo  -- curl -i http://demo-app-v1:8080/hello
```
In Both cases Output will be:
```shell
HTTP/1.1 403 Forbidden
content-length: 19
content-type: text/plain
date: Wed, 15 Mar 2023 08:09:58 GMT
server: envoy
x-envoy-upstream-service-time: 3

RBAC: access denied
```

4. Invoke HTTP Call using the user header with value 'admin-zvi':
```shell
 oc exec rest-api-client -n demo  -- curl -i http://demo-app-v1:8080/hello -H 'user: admin-zvi'
```
Output:
```shell
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 5
date: Wed, 15 Mar 2023 08:12:49 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
```
## Allow Access from same namespace only for a specific Service account.
1. Delete previous `AuthorizationPolicy`
```shell
oc delete -f resources/authorization-policy-allow-based-on-header.yaml
```

2. Apply new `AuthorizationPolicy` That will authorize only a particular service account on the demo namespace to invoke the demo-app service:
```shell
oc apply -f resources/authorization-policy-allow-custom-workload-sa.yaml
```

3. Now try to invoke the service from the rest-client-pod that is running inside the mesh with "default" `ServiceAccount`:
```shell
oc exec rest-api-client -n demo  -- curl -i http://demo-app-v1:8080/hello
```
Output:
```shell
403 Forbidden
content-length: 19
content-type: text/plain
date: Wed, 15 Mar 2023 08:18:02 GMT
server: envoy
x-envoy-upstream-service-time: 1

RBAC: access denied
```
4. Create SA demo-app-sa
```shell
 oc create serviceaccount demo-app-sa
```

5. Delete rest-api-client pod
```shell
oc delete pod rest-api-client  
```

6. Redeploy rest-api-client pod with new `ServiceAccount` demo-app-sa:
```shell
oc apply -f resources/rest-client-pod-sidecar-custom-sa.yaml
```
7. Try now 
```shell
oc exec rest-api-client -n demo  -- curl -i http://demo-app-v1:8080/hello
```
Output:
```shell
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 195
date: Wed, 15 Mar 2023 08:29:53 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
```