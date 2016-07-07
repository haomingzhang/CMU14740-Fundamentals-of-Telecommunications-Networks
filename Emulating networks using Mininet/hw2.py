#!/usr/bin/python

from mininet.topo import Topo
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI

class HW2Topo(Topo):
    def build(self):
        switches = []
        hosts = []
        for h in range(8):
            hosts.append(self.addHost('h%s' %(h+1), cpu=0.5/8))
        for s in range(6):
            switches.append(self.addSwitch('s%s' %(s+1)))
        #S1-S2
        self.addLink(switches[0], switches[1], bw=10, delay='1ms', loss=3)
        #S1-S3
        self.addLink(switches[0], switches[2], bw=15, delay='2ms', loss=2)
        #S2-S4
        self.addLink(switches[1], switches[3], bw=20, delay='4ms', loss=1)
        #S3-S5
        self.addLink(switches[2], switches[4], bw=20, delay='4ms', loss=1)
        #S5-S6
        self.addLink(switches[4], switches[5], bw=40, delay='10ms', loss=2)
        #S1-h1
        self.addLink(switches[0], hosts[0])
        #S2-h2
        self.addLink(switches[1], hosts[1])
        #S3-h3
        self.addLink(switches[2], hosts[2])
        #S4-h4
        self.addLink(switches[3], hosts[3])
        #S4-h5
        self.addLink(switches[3], hosts[4])
        #S5-h6
        self.addLink(switches[4], hosts[5])
        #S6-h7
        self.addLink(switches[5], hosts[6])
        #S6-h8
        self.addLink(switches[5], hosts[7])

def perfTest():
    "Create network and run simple performance tests"
    topo = HW2Topo()
    net = Mininet(topo=topo,host=CPULimitedHost, link=TCLink)
    net.start()
    print "Dumping host connections"
    dumpNodeConnections(net.hosts)
    #net.pingAll()

    #ping
    for host1 in range(1,8):
        for host2 in range(host1+1,9):
            hosts=net.get('h%s' %host1,'h%s' %host2)
            for i in range(10):
                net.ping(hosts=hosts)
    #TCP
    for host1 in range(1,8):
        for host2 in range(host1+1,9):
            hosts=net.get('h%s' %host1,'h%s' %host2)
            net.iperf(hosts=hosts)
    
    #UDP
    for host1 in range(1,8):
        for host2 in range(host1+1,9):
            hosts=net.get('h%s' %host1,'h%s' %host2)

            print 'h%s' %host2 + '->' +' h%s' %host1
            #server
            result=hosts[0].cmd('iperf -u -s &')
            pid = hosts[0].cmd('echo $!')
            print result
            #client
            result1=hosts[1].cmd('iperf -c ' + hosts[0].IP() + ' -u -b 15000000')
            print result1
            hosts[0].cmd('kill -9 $pid')

            print 'h%s' %host1 + '->' +' h%s' %host2
            #server
            result=hosts[1].cmd('iperf -u -s &')
            pid = hosts[1].cmd('echo $!')
            print result
            #client
            result1=hosts[0].cmd('iperf -c ' + hosts[1].IP() + ' -u -b 15000000')
            print result1
            hosts[1].cmd('kill -9 $pid')

    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    perfTest()
