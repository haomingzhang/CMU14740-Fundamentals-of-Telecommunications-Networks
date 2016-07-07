from pox.core import core
from pox.lib.packet.ethernet import ethernet
from pox.lib.addresses import EthAddr, IPAddr
import pox.openflow.libopenflow_01 as of
import random


log = core.getLogger()

hostsPort = [1,2,3,4,5,6,7]
serversPort = [8,9,10]
idleTimeout = 40

loadBalancerMac = EthAddr('00:00:00:00:00:FE')
loadBalancerIP = IPAddr('10.0.0.254')

class LoadBalancer (object):
  def __init__ (self, connection):
    self.connection = connection
    connection.addListeners(self)

    # Servers
    self.serversIP = {}
    self.serversMac = {}
    self.serverPortSet = set()
    self.ipToPort = {}


    for i in serversPort:
      self.serversIP[i] = IPAddr('10.0.0.'+str(i))
      self.serversMac[i] = EthAddr('00:00:00:00:00:0'+str(hex(i)[-1]))
      self.serverPortSet.add(i)
      self.ipToPort[self.serversIP[i]] = i

    for i in hostsPort:
      self.ipToPort[IPAddr('10.0.0.'+str(i))] = i

    log.debug('Start Controlling!')

  def set_flow_match (self, fromPort, fromIP, toIP):
    msg = of.ofp_flow_mod()
    msg.match.in_port = fromPort
    msg.match.nw_src = fromIP
    msg.match.nw_dst = toIP
    msg.match.dl_type = ethernet.IP_TYPE
    msg.buffer_id = None
    msg.idle_timeout = idleTimeout
    return msg

  def forward_packet (self, packetIn, outPort):
    """
    Copied from sample code
    """
    msg = of.ofp_packet_out()
    msg.data = packetIn
    msg.actions.append(of.ofp_action_nw_addr.set_src(loadBalancerIP))
    msg.actions.append(of.ofp_action_output(port = outPort))
    self.connection.send(msg)


  def _handle_PacketIn (self, event):
    packet = event.parsed
    if not packet.parsed:
      log.warning('Ignoring incomplete packet')
      return

    #log.debug('Event packet from ' + str(packet.src) + ' to ' + str(packet.dst))



    if packet.type==packet.IP_TYPE:
      packetIn = event.ofp
      ipPacket = packet.payload
      srcIP = ipPacket.srcip
      dstIP = ipPacket.dstip
      # If the packet is from a client to the load balancer
      if (event.port not in self.serverPortSet) and (packet.dst==loadBalancerMac) and (dstIP==loadBalancerIP):
        clientPort = event.port
        clientIP = srcIP
        clientMac = packet.src
        serverPort = random.choice(serversPort)
        serverIP = self.serversIP[serverPort]
        serverMac = self.serversMac[serverPort]

        log.debug('Event packet from ' + str(srcIP) + ' to ' + str(serverIP))

        #Flow table from client to the server
        msg = self.set_flow_match(clientPort, clientIP, loadBalancerIP)
        msg.data = packetIn
        msg.actions.append(of.ofp_action_nw_addr.set_dst(serverIP))
        msg.actions.append(of.ofp_action_dl_addr.set_src(loadBalancerMac))
        msg.actions.append(of.ofp_action_dl_addr.set_dst(serverMac))
        msg.actions.append(of.ofp_action_output(port = serverPort))
        self.connection.send(msg)
        #Flow table from server to the client
        msg = self.set_flow_match(serverPort, serverIP, clientIP)
        msg.actions.append(of.ofp_action_nw_addr.set_src(loadBalancerIP))
        msg.actions.append(of.ofp_action_dl_addr.set_src(loadBalancerMac))
        msg.actions.append(of.ofp_action_dl_addr.set_dst(clientMac))
        msg.actions.append(of.ofp_action_output(port = clientPort))
        self.connection.send(msg)


      elif self.ipToPort.has_key(dstIP):
        #forward the packet
        log.debug('Forward a packet from '+str(srcIP)+' to '+str(dstIP))
        self.forward_packet(packetIn, self.ipToPort[dstIP])
    else:
      log.warning('Ignore a packet from '+str(packet.src))
      

def launch ():
  def start_switch (event):
    log.debug("Controlling %s" % (event.connection,))
    LoadBalancer(event.connection)
  core.openflow.addListenerByName("ConnectionUp", start_switch)
